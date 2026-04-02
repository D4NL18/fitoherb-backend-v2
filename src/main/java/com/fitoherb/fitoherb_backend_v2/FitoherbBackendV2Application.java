package com.fitoherb.fitoherb_backend_v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class FitoherbBackendV2Application {

	public static void main(String[] args) {
		SpringApplication.run(FitoherbBackendV2Application.class, args);
	}

}
