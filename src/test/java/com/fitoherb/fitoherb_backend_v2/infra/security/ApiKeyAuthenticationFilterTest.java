package com.fitoherb.fitoherb_backend_v2.infra.security;

import com.fitoherb.fitoherb_backend_v2.entities.ApiKey;
import com.fitoherb.fitoherb_backend_v2.services.ApiKeyService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private ApiKeyAuthenticationFilter filter;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("chave valida autentica com ROLE_ADMIN")
    void validKeyGrantsAdmin() throws Exception {
        ApiKey key = new ApiKey();
        key.setName("claude-x");
        when(apiKeyService.authenticate("fh_valid")).thenReturn(Optional.of(key));
        var request = new MockHttpServletRequest("GET", "/products");
        request.addHeader("X-API-Key", "fh_valid");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("api-key:claude-x", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("chave invalida retorna 401 e interrompe a cadeia")
    void invalidKeyReturns401() throws Exception {
        when(apiKeyService.authenticate("fh_bad")).thenReturn(Optional.empty());
        var request = new MockHttpServletRequest("GET", "/products");
        request.addHeader("X-API-Key", "fh_bad");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("sem header segue o fluxo normal (JWT) sem autenticar")
    void noHeaderPassesThrough() throws Exception {
        var request = new MockHttpServletRequest("GET", "/products");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(apiKeyService);
    }

    @Test
    @DisplayName("gestao de chaves nao aceita API key (anti escalonamento)")
    void apiKeyCannotManageKeys() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api-keys");
        request.addHeader("X-API-Key", "fh_valid");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verifyNoInteractions(apiKeyService);
    }
}
