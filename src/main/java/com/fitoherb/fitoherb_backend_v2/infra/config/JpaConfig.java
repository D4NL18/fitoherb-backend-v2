package com.fitoherb.fitoherb_backend_v2.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@ConditionalOnProperty(
        name = "org.springframework.boot.webmvc.test.autoconfigure.WebMvcTestContextBootstrapper",
        havingValue = "false",
        matchIfMissing = true
)
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig {
}
