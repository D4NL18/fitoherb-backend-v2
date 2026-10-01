package com.fitoherb.fitoherb_backend_v2.infra.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseConstraintInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role >= 0 AND role <= 2)");
            log.info("Restrição de verificação 'users_role_check' atualizada com sucesso no banco de dados.");
        } catch (Exception e) {
            log.warn("Não foi possível atualizar a restrição users_role_check: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN birth_date DROP NOT NULL");
            log.info("Coluna legada 'birth_date' ajustada para nullable na tabela users.");
        } catch (Exception e) {
            log.debug("Coluna 'birth_date' não existe ou já é nullable: {}", e.getMessage());
        }
    }
}
