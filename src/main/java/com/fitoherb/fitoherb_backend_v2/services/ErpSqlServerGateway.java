package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.exceptions.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ErpSqlServerGateway implements ErpCatalogGateway {

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    private static final String ALLOWLIST_CATALOG_QUERY =
            "SELECT " +
            "  CAST(p.Id_Pro AS BIGINT) AS erp_code, " +
            "  LTRIM(RTRIM(p.descricao_pro)) AS description, " +
            "  NULLIF(LTRIM(RTRIM(p.codbarra_pro)), '') AS ean, " +
            "  NULLIF(LTRIM(RTRIM(m.Nome_Marc)), '') AS brand_name, " +
            "  NULLIF(LTRIM(RTRIM(t.descricao_tppro)), '') AS category_label, " +
            "  CAST(ISNULL(p.inativo_pro, 0) AS BIT) AS inativo_pro, " +
            "  CASE WHEN e.Codigo IS NOT NULL THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END AS in_externo_01 " +
            "FROM [dbo].[Produtos] p WITH (NOLOCK) " +
            "LEFT JOIN [dbo].[Marca] m WITH (NOLOCK) ON m.Id_Marc = p.Marca_Pro " +
            "LEFT JOIN [dbo].[TipoProd] t WITH (NOLOCK) ON t.cod_tppro = p.tipoprod_pro " +
            "LEFT JOIN [Externo01].[Produto] e WITH (NOLOCK) ON e.Codigo = p.Id_Pro " +
            "WHERE p.Empresa_Pro = 1 " +
            "  AND UPPER(LTRIM(RTRIM(ISNULL(t.descricao_tppro, '')))) NOT IN (" +
            "    'BRINDES', 'BRINDES/INV', 'IMOBILIZADO', 'AMOSTRA GRATIS', 'DIVERSOS', 'LINHA DIVERSOS'" +
            "  ) " +
            "ORDER BY p.Id_Pro ASC";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final ConnectionSupplier connectionSupplier;

    @Autowired
    public ErpSqlServerGateway(
            @Value("${app.erp.sqlserver.host:}") String host,
            @Value("${app.erp.sqlserver.port:1433}") int port,
            @Value("${app.erp.sqlserver.database:FITOHERB}") String database,
            @Value("${app.erp.sqlserver.username:}") String username,
            @Value("${app.erp.sqlserver.password:}") String password
    ) {
        this(host, port, database, username, password, null);
    }

    public ErpSqlServerGateway(
            String host,
            int port,
            String database,
            String username,
            String password,
            ConnectionSupplier connectionSupplier
    ) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.connectionSupplier = connectionSupplier != null ? connectionSupplier : this::openJdbcConnection;
    }

    public String buildJdbcUrl(String host, int port, String database) {
        return String.format("jdbc:jtds:sqlserver://%s:%d/%s;ssl=off;loginTimeout=5;socketTimeout=15", host, port, database);
    }

    private Connection openJdbcConnection() throws SQLException {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver jTDS não encontrado no classpath", e);
        }
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(buildJdbcUrl(host, port, database), username, password);
    }

    @Override
    public List<ErpCatalogItemSnapshot> fetchFullCatalog() {
        if (host == null || host.isBlank() || username == null || username.isBlank()) {
            throw new ServiceUnavailableException(
                    "ERP_CATALOG_UNAVAILABLE",
                    "Base de referência do ERP Atrio indisponível no momento para validar os códigos informados."
            );
        }

        List<ErpCatalogItemSnapshot> items = new ArrayList<>();
        try (Connection conn = connectionSupplier.get();
             PreparedStatement stmt = conn.prepareStatement(ALLOWLIST_CATALOG_QUERY);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(ErpCatalogItemSnapshot.builder()
                        .erpCode(rs.getLong("erp_code"))
                        .description(rs.getString("description"))
                        .ean(rs.getString("ean"))
                        .brandName(rs.getString("brand_name"))
                        .categoryLabel(rs.getString("category_label"))
                        .inativoPro(rs.getBoolean("inativo_pro"))
                        .inExterno01(rs.getBoolean("in_externo_01"))
                        .build());
            }
            return items;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Falha ao consultar catálogo somente-leitura no SQL Server ERP Atrio: {}", e.getClass().getSimpleName());
            throw new ServiceUnavailableException(
                    "ERP_CATALOG_UNAVAILABLE",
                    "Base de referência do ERP Atrio indisponível no momento para validar os códigos informados."
            );
        }
    }
}
