package com.fitoherb.fitoherb_backend_v2.infra.security;

import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.enums.UserRole;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import com.fitoherb.fitoherb_backend_v2.services.TokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock private TokenService tokenService;
    @Mock private UserRepository userRepository;
    @Mock private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Segurança / LGPD: SecurityFilter não pode logar username/email/PII")
    class PiiLoggingTests {

        @Test
        @DisplayName("Não deve imprimir nem logar e-mail/username do usuário autenticado em stdout durante validação de JWT")
        void shouldNotLogUserEmailOrPiiOnStdout() throws Exception {
            // Arrange
            String secretEmail = "operador.privado@fitoherb.com";
            User user = new User();
            user.setEmail(secretEmail);
            user.setRole(UserRole.ADMIN);

            when(tokenService.validateToken("valid-jwt")).thenReturn(secretEmail);
            when(userRepository.findByEmail(secretEmail)).thenReturn(Optional.of(user));

            SecurityFilter filter = new SecurityFilter(tokenService, userRepository);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products");
            request.addHeader("Authorization", "Bearer valid-jwt");
            MockHttpServletResponse response = new MockHttpServletResponse();

            PrintStream originalOut = System.out;
            ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
            System.setOut(new PrintStream(capturedOut));

            try {
                // Act
                filter.doFilter(request, response, filterChain);
            } finally {
                System.setOut(originalOut);
            }

            // Assert
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            String stdout = capturedOut.toString();
            assertFalse(stdout.contains(secretEmail), "SecurityFilter vazou e-mail (PII) em System.out: " + stdout);
            assertFalse(stdout.toLowerCase().contains("email:"), "SecurityFilter não deve logar campo email em stdout");
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("M2M Fail-Closed: SyncSchedulerAuthFilter (X-Sync-Scheduler-Token)")
    class SyncSchedulerAuthFilterTests {

        @Test
        @DisplayName("Fail-closed: Deve retornar 503 quando SYNC_SCHEDULER_TOKEN não estiver configurado")
        void shouldReturn503FailClosedWhenTokenNotConfigured() throws Exception {
            // Arrange
            SyncSchedulerAuthFilter filter = new SyncSchedulerAuthFilter("");
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/erp-products/sync/daily");
            request.addHeader("X-Sync-Scheduler-Token", "any-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilter(request, response, filterChain);

            // Assert
            assertEquals(503, response.getStatus());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("Deve retornar 401 quando header X-Sync-Scheduler-Token for inválido")
        void shouldReturn401WhenHeaderTokenInvalid() throws Exception {
            // Arrange
            SyncSchedulerAuthFilter filter = new SyncSchedulerAuthFilter("expected-secret-token");
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/erp-products/sync/daily");
            request.addHeader("X-Sync-Scheduler-Token", "wrong-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilter(request, response, filterChain);

            // Assert
            assertEquals(401, response.getStatus());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("Deve autenticar ROLE_SYNC_SCHEDULER quando header X-Sync-Scheduler-Token coincidir em tempo constante")
        void shouldAuthenticateRoleSyncSchedulerWhenTokenMatches() throws Exception {
            // Arrange
            SyncSchedulerAuthFilter filter = new SyncSchedulerAuthFilter("expected-secret-token");
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/erp-products/sync/daily");
            request.addHeader("X-Sync-Scheduler-Token", "expected-secret-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilter(request, response, filterChain);

            // Assert
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SYNC_SCHEDULER".equals(a.getAuthority())));
            verify(filterChain).doFilter(request, response);
        }
    }
}
