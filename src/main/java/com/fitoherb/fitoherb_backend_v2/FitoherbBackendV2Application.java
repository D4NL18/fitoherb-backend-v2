package com.fitoherb.fitoherb_backend_v2;

import com.fitoherb.fitoherb_backend_v2.infra.config.JpaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Import(JpaConfig.class)
@EnableScheduling
public class FitoherbBackendV2Application {

	public static void main(String[] args) {
		SpringApplication.run(FitoherbBackendV2Application.class, args);
	}

}
