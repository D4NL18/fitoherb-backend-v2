package com.fitoherb.fitoherb_backend_v2.infra.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class FitoherbAiClient {

    private final RestClient restClient;

    public FitoherbAiClient(@Value("${fitoherb.ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> optimizeRoute(Map<String, Object> payload) {
        log.info("Enviando requisição de otimização de rota para fitoherb-ai");
        return this.restClient.post()
                .uri("/api/v1/routing/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkHealth() {
        return this.restClient.get()
                .uri("/api/v1/health")
                .retrieve()
                .body(Map.class);
    }
}
