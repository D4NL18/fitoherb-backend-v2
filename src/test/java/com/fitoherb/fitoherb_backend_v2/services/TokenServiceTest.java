package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.exceptions.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private TokenService tokenService;
    private static final String SECRET = "minha-chave-secreta-para-testes-fitoherb-123";
    private User user;

    @BeforeEach
    void setup() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);

        user = new User();
        user.setEmail("daniel@fitoherb.com");
    }

    @Nested
    @DisplayName("Testes de Geração de Token")
    class GenerationTests {

        @Test
        void generateTokenSuccess() {
            String token = tokenService.generateToken(user);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertEquals(3, token.split("\\.").length);
        }
    }

    @Nested
    @DisplayName("Testes de Validação de Token")
    class ValidationTests {

        @Test
        void validateTokenSuccess() {
            String token = tokenService.generateToken(user);
            String subject = tokenService.validateToken(token);

            assertEquals(user.getEmail(), subject);
        }

        @Test
        void validateEmptyToken() {
            assertThrows(InvalidTokenException.class, () -> tokenService.validateToken(""));
            assertThrows(InvalidTokenException.class, () -> tokenService.validateToken(null));
        }

        @Test
        void validateMalformedToken() {
            assertThrows(InvalidTokenException.class, () -> tokenService.validateToken("token.invalido.aqui"));
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Robustez")
    class SecurityTests {

        @Test
        void invalidSignature() {
            String token = tokenService.generateToken(user);

            TokenService hackerService = new TokenService();
            ReflectionTestUtils.setField(hackerService, "secret", "wrong-secret");

            assertThrows(InvalidTokenException.class, () -> hackerService.validateToken(token));
        }

        @Test
        void tamperedPayload() {
            String token = tokenService.generateToken(user);
            String tamperedToken = token.substring(0, token.indexOf(".") + 5) + "X" + token.substring(token.indexOf(".") + 6);

            assertThrows(InvalidTokenException.class, () -> tokenService.validateToken(tamperedToken));
        }

        @Test
        void expiredToken() {
            String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhdXRoLWFwaSIsInN1YiI6ImRhbmllbEBmaXRvaGVyYi5jb20iLCJleHAiOjE2NTAwMDAwMDB9.invalid";
            assertThrows(InvalidTokenException.class, () -> tokenService.validateToken(expiredToken));
        }

        @Test
        void robustSecret() {
            ReflectionTestUtils.setField(tokenService, "secret", "");
            assertThrows(IllegalArgumentException.class, () -> tokenService.generateToken(user));
        }
    }
}