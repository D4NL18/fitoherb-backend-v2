package com.fitoherb.fitoherb_backend_v2.infra.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Component
public class AiCatalogRefreshClient {

    private static final String REFRESH_URI = "/api/v1/chat/catalog/refresh";
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Refresh-Token";

    private final RestClient restClient;
    private final String internalRefreshToken;

    @Autowired
    public AiCatalogRefreshClient(
            @Value("${fitoherb.ai.service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${app.ai.internal-refresh-token:}") String internalRefreshToken
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory)
                .build();
        this.internalRefreshToken = internalRefreshToken;
    }

    public AiCatalogRefreshClient(RestClient restClient, String internalRefreshToken) {
        this.restClient = restClient;
        this.internalRefreshToken = internalRefreshToken;
    }

    public boolean refreshCatalog() {
        if (internalRefreshToken == null || internalRefreshToken.isBlank()) {
            log.debug("Token interno de refresh do fitoherb-ai ausente; ignorando webhook pós-sync.");
            return false;
        }
        try {
            restClient.post()
                    .uri(REFRESH_URI)
                    .header(HEADER_INTERNAL_TOKEN, internalRefreshToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Refresh best-effort do CatalogIndex no fitoherb-ai não pôde ser concluído: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
