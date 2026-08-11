package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
public class DataMigrationController {

    private final ProductService productService;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/product-slugs")
    public ResponseEntity<String> migrateProductSlugs() {
        productService.migrateAllProductSlugs();
        return ResponseEntity.ok("Todos os slugs de produtos foram migrados com sucesso para o novo formato!");
    }

    @PostMapping("/drop-name-constraint")
    public ResponseEntity<String> dropNameConstraint() {
        String sql = "DO $$ " +
                     "DECLARE " +
                     "    const_name text; " +
                     "BEGIN " +
                     "    SELECT constraint_name INTO const_name " +
                     "    FROM information_schema.table_constraints " +
                     "    WHERE table_name = 'products' AND constraint_type = 'UNIQUE' " +
                     "      AND constraint_name IN ( " +
                     "          SELECT constraint_name " +
                     "          FROM information_schema.constraint_column_usage " +
                     "          WHERE table_name = 'products' AND column_name = 'name' " +
                     "      ); " +
                     "    IF const_name IS NOT NULL THEN " +
                     "        EXECUTE 'ALTER TABLE products DROP CONSTRAINT ' || const_name; " +
                     "    END IF; " +
                     "END $$;";
        jdbcTemplate.execute(sql);
        return ResponseEntity.ok("Constraint UNIQUE da coluna 'name' removida com sucesso do banco de dados!");
    }
}
