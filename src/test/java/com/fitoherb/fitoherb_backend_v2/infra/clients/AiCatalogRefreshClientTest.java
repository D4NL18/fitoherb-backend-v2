package com.fitoherb.fitoherb_backend_v2.infra.clients;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiCatalogRefreshClientTest {

    @Test
    @DisplayName("P-410 & P-411: Deve enviar POST /api/v1/chat/catalog/refresh com header X-Internal-Refresh-Token")
    void shouldSendCatalogRefreshWithInternalTokenHeader() {
        // Arrange
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiCatalogRefreshClient client = new AiCatalogRefreshClient(builder.build(), "super-secret-refresh-token");

        server.expect(requestTo("http://localhost:8000/api/v1/chat/catalog/refresh"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Refresh-Token", "super-secret-refresh-token"))
                .andRespond(withSuccess("{\"status\":\"refreshed\"}", MediaType.APPLICATION_JSON));

        // Act
        boolean refreshed = client.refreshCatalog();

        // Assert
        assertTrue(refreshed);
        server.verify();
    }

    @Test
    @DisplayName("Fail-closed / Best-effort: Não deve chamar endpoint quando token interno estiver ausente e deve retornar false")
    void shouldReturnFalseWithoutCallingWhenTokenBlank() {
        // Arrange
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiCatalogRefreshClient client = new AiCatalogRefreshClient(builder.build(), "");

        // Act
        boolean refreshed = client.refreshCatalog();

        // Assert
        assertFalse(refreshed);
        server.verify();
    }

    @Test
    @DisplayName("Resiliência pós-commit: Deve retornar false sem lançar exceção se fitoherb-ai retornar 503")
    void shouldReturnFalseGracefullyWhenAiServiceReturns503() {
        // Arrange
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiCatalogRefreshClient client = new AiCatalogRefreshClient(builder.build(), "valid-token");

        server.expect(requestTo("http://localhost:8000/api/v1/chat/catalog/refresh"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServiceUnavailable());

        // Act
        boolean refreshed = assertDoesNotThrow(client::refreshCatalog);

        // Assert
        assertFalse(refreshed);
        server.verify();
    }
}
