package com.fitoherb.fitoherb_backend_v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
@EnableScheduling
public class FitoherbBackendV2Application {

	public static void main(String[] args) {
		SpringApplication.run(FitoherbBackendV2Application.class, args);
	}

}
