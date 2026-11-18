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

        initializeErpCatalogSchema();
    }

    private void initializeErpCatalogSchema() {
        try {
            jdbcTemplate.execute("ALTER TABLE public.products ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE public.products ADD COLUMN IF NOT EXISTS curation_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETE'");
            jdbcTemplate.execute("UPDATE public.products SET active = TRUE WHERE active IS NULL");
            jdbcTemplate.execute("UPDATE public.products SET curation_status = 'COMPLETE' WHERE curation_status IS NULL OR curation_status = 'COMPLETED'");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS public.product_skus (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "product_id VARCHAR(255) NOT NULL REFERENCES public.products(id) ON DELETE CASCADE, " +
                    "erp_code BIGINT NOT NULL UNIQUE, " +
                    "flavour VARCHAR(100), " +
                    "presentation VARCHAR(100), " +
                    "normalized_variant_key VARCHAR(220) NOT NULL, " +
                    "ean VARCHAR(20), " +
                    "sort_order INTEGER NOT NULL DEFAULT 0, " +
                    "erp_inactive BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "erp_inactive_confirmed BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "erp_last_verified_at TIMESTAMP, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT NOW(), " +
                    "updated_at TIMESTAMP, " +
                    "created_by VARCHAR(255), " +
                    "updated_by VARCHAR(255), " +
                    "CONSTRAINT uk_product_skus_product_variant UNIQUE (product_id, normalized_variant_key))"
            );
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_product_skus_product_id_sort ON public.product_skus (product_id, sort_order, id)");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS public.erp_sync_state (" +
                    "id SMALLINT PRIMARY KEY DEFAULT 1, " +
                    "baseline_cutoff_at TIMESTAMP NULL, " +
                    "auto_sync_enabled BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "last_successful_sync_at TIMESTAMP NULL, " +
                    "in_progress BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "lock_acquired_at TIMESTAMP NULL, " +
                    "active_run_id VARCHAR(36) NULL, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT NOW())"
            );
            jdbcTemplate.execute("INSERT INTO public.erp_sync_state (id, auto_sync_enabled, in_progress, updated_at) VALUES (1, TRUE, FALSE, NOW()) ON CONFLICT (id) DO NOTHING");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS public.erp_sync_runs (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "trigger_type VARCHAR(30) NOT NULL, " +
                    "triggered_by VARCHAR(150), " +
                    "status VARCHAR(30) NOT NULL, " +
                    "started_at TIMESTAMP NOT NULL DEFAULT NOW(), " +
                    "finished_at TIMESTAMP, " +
                    "deactivated_count INTEGER NOT NULL DEFAULT 0, " +
                    "reactivated_count INTEGER NOT NULL DEFAULT 0, " +
                    "partial_variant_updated_count INTEGER NOT NULL DEFAULT 0, " +
                    "created_count INTEGER NOT NULL DEFAULT 0, " +
                    "skipped_ignored_count INTEGER NOT NULL DEFAULT 0, " +
                    "error_summary VARCHAR(500))"
            );
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS public.erp_ignored_catalog_codes (" +
                    "erp_code BIGINT PRIMARY KEY, " +
                    "source VARCHAR(30) NOT NULL DEFAULT 'ADMIN_IGNORED', " +
                    "product_name_snapshot VARCHAR(255), " +
                    "reason VARCHAR(255), " +
                    "ignored_by VARCHAR(150) NOT NULL, " +
                    "ignored_at TIMESTAMP NOT NULL DEFAULT NOW())"
            );
        } catch (Exception e) {
            log.debug("Inicialização idempotente de schema ERP ignorada ou já aplicada: {}", e.getMessage());
        }
    }
}
