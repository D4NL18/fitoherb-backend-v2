package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.exceptions.ServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpCatalogGatewayTest {

    @Mock private ErpCatalogGateway erpCatalogGateway;

    @Nested
    @DisplayName("ErpSqlServerGateway: jTDS ssl=off, Allowlist LGPD e Adendo C-23")
    class SqlServerGatewayTests {

        @Test
        @DisplayName("Deve construir URL JDBC jTDS com ssl=off e falhar de forma fechada quando credenciais ausentes")
        void shouldBuildJtdsUrlWithSslOffAndFailClosedWithoutCredentials() {
            // Arrange
            ErpSqlServerGateway unconfiguredGateway = new ErpSqlServerGateway("", 1433, "FITOHERB", "", "");

            // Act & Assert
            ServiceUnavailableException ex = assertThrows(
                    ServiceUnavailableException.class,
                    unconfiguredGateway::fetchFullCatalog
            );
            assertEquals("ERP_CATALOG_UNAVAILABLE", ex.getCode());
            assertTrue(unconfiguredGateway.buildJdbcUrl("127.0.0.1", 1433, "FITOHERB").startsWith("jdbc:jtds:sqlserver://"));
            assertTrue(unconfiguredGateway.buildJdbcUrl("127.0.0.1", 1433, "FITOHERB").contains("ssl=off"));
        }

        @Test
        @DisplayName("C-23 & LGPD: Query SQL deve usar WITH (NOLOCK), Allowlist explícita e excluir TipoProd não comerciais")
        void shouldQueryOnlyAllowlistColumnsAndExcludeNonCommercialTipoProd() throws Exception {
            // Arrange
            Connection conn = mock(Connection.class);
            PreparedStatement stmt = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            when(conn.prepareStatement(anyString())).thenReturn(stmt);
            when(stmt.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("erp_code")).thenReturn(131476L);
            when(rs.getString("description")).thenReturn("100% PURE WHEY 900G BAUNILHA");
            when(rs.getString("ean")).thenReturn("7896311770011");
            when(rs.getString("brand_name")).thenReturn("MAX TITANIUM");
            when(rs.getString("category_label")).thenReturn("PROTEINAS");
            when(rs.getBoolean("inativo_pro")).thenReturn(false);
            when(rs.getBoolean("in_externo_01")).thenReturn(true);

            ErpSqlServerGateway gateway = new ErpSqlServerGateway("erp.local", 1433, "FITOHERB", "erp_readonly", "secret", () -> conn);

            // Act
            List<ErpCatalogItemSnapshot> items = gateway.fetchFullCatalog();

            // Assert
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(conn).prepareStatement(sqlCaptor.capture());
            String sql = sqlCaptor.getValue();
            String upperSql = sql.toUpperCase();

            assertFalse(upperSql.contains("SELECT *"), "Proibido SELECT * pela Allowlist LGPD");
            assertFalse(upperSql.contains("ESTOQUE"), "Proibido consultar Estoque (P-302, P-408)");
            assertFalse(upperSql.contains("CLIENTES"), "Proibido consultar Clientes (LGPD)");
            assertFalse(upperSql.contains("DBO.FORNECEDOR"), "Proibido consultar dbo.Fornecedor PF/MEI (LGPD)");
            assertFalse(upperSql.contains("ULTIMO CUSTO"), "Proibido consultar custos");
            assertTrue(upperSql.contains("WITH (NOLOCK)"));
            assertTrue(upperSql.contains("EMPRESA_PRO = 1"));
            assertTrue(upperSql.contains("BRINDES"));
            assertTrue(upperSql.contains("BRINDES/INV"));
            assertTrue(upperSql.contains("IMOBILIZADO"));
            assertTrue(upperSql.contains("AMOSTRA GRATIS"));
            assertTrue(upperSql.contains("DIVERSOS"));
            assertTrue(upperSql.contains("LINHA DIVERSOS"));
            assertTrue(upperSql.contains("T.ID_TPPRO = P.TIPO_PRO"), "LEFT JOIN TipoProd deve usar t.Id_TpPro = p.tipo_pro");
            assertTrue(upperSql.contains("T.EMPRESA_TPPRO = P.EMPRESA_PRO"), "LEFT JOIN TipoProd deve usar t.Empresa_TpPro = p.Empresa_Pro");
            assertTrue(upperSql.contains("M.ID_MARC = P.MARCA_PRO"), "LEFT JOIN Marca deve usar m.ID_Marc = p.Marca_Pro");
            assertTrue(upperSql.contains("M.EMPRESA_MARC = P.EMPRESA_PRO"), "LEFT JOIN Marca deve usar m.Empresa_Marc = p.Empresa_Pro");
            assertFalse(upperSql.contains("COD_TPPRO"), "Coluna inexistente cod_tppro deve ser removida");
            assertFalse(upperSql.contains("TIPOPROD_PRO"), "Coluna inexistente tipoprod_pro deve ser removida");
            assertFalse(upperSql.contains("[EXTERNO01].[PRODUTO]"), "JOIN com [Externo01].[Produto] deve ser removido");
            assertTrue(upperSql.contains("CAST(0 AS BIT) AS IN_EXTERNO_01"), "Deve projetar CAST(0 AS BIT) AS in_externo_01");

            assertEquals(1, items.size());
            assertEquals(131476L, items.get(0).getErpCode());
        }

        @Test
        @DisplayName("Fail-closed: Deve lançar ServiceUnavailableException (503) quando SQL Server falhar")
        void shouldFailClosedWhenSqlExceptionOccurs() throws Exception {
            // Arrange
            ErpSqlServerGateway gateway = new ErpSqlServerGateway(
                    "erp.local", 1433, "FITOHERB", "erp_readonly", "secret",
                    () -> { throw new SQLException("Connection timed out"); }
            );

            // Act & Assert
            ServiceUnavailableException ex = assertThrows(
                    ServiceUnavailableException.class,
                    gateway::fetchFullCatalog
            );
            assertEquals("ERP_CATALOG_UNAVAILABLE", ex.getCode());
        }
    }

    @Nested
    @DisplayName("ErpCatalogCacheService: Cache em RAM com TTL curto e Fail-Closed")
    class RamCacheTests {

        @Test
        @DisplayName("Deve reutilizar snapshot em RAM dentro do TTL curto e recarregar após expiração")
        void shouldUseRamCacheWithinTtlAndRefreshAfterExpiration() {
            // Arrange
            AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-18T10:00:00Z"));
            Clock mutableClock = new Clock() {
                @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
                @Override public Clock withZone(ZoneId zone) { return this; }
                @Override public Instant instant() { return now.get(); }
            };

            ErpCatalogItemSnapshot item1 = ErpCatalogItemSnapshot.builder()
                    .erpCode(131476L).description("WHEY 900G BAUNILHA").inativoPro(false).inExterno01(true).build();
            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(List.of(item1));

            ErpCatalogCacheService cacheService = new ErpCatalogCacheService(erpCatalogGateway, 900L, mutableClock);

            // Act 1: Cold load
            Optional<ErpCatalogItemSnapshot> first = cacheService.findByErpCode(131476L);
            // Act 2: Within TTL (5 min later)
            now.set(Instant.parse("2026-11-18T10:05:00Z"));
            Optional<ErpCatalogItemSnapshot> second = cacheService.findByErpCode(131476L);
            // Act 3: Past TTL (16 min later)
            now.set(Instant.parse("2026-11-18T10:16:00Z"));
            Optional<ErpCatalogItemSnapshot> third = cacheService.findByErpCode(131476L);

            // Assert
            assertTrue(first.isPresent());
            assertTrue(second.isPresent());
            assertTrue(third.isPresent());
            verify(erpCatalogGateway, times(2)).fetchFullCatalog();
        }

        @Test
        @DisplayName("Fail-closed: Deve propagar 503 ERP_CATALOG_UNAVAILABLE quando cache frio e ERP indisponível")
        void shouldFailClosedWhenCacheColdAndGatewayUnavailable() {
            // Arrange
            when(erpCatalogGateway.fetchFullCatalog())
                    .thenThrow(new ServiceUnavailableException("ERP_CATALOG_UNAVAILABLE", "ERP offline"));
            ErpCatalogCacheService cacheService = new ErpCatalogCacheService(
                    erpCatalogGateway, 900L, Clock.systemUTC()
            );

            // Act & Assert
            ServiceUnavailableException ex = assertThrows(
                    ServiceUnavailableException.class,
                    () -> cacheService.findByErpCode(131476L)
            );
            assertEquals("ERP_CATALOG_UNAVAILABLE", ex.getCode());
        }
    }
}
