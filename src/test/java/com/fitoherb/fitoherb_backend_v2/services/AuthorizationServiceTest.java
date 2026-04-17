package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.LoginReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.RegisterReq;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.mappers.AuthMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthMapper authMapper;
    @Mock private TokenService tokenService;
    @Mock private MailService mailService;
    @Mock private ObjectProvider<AuthenticationManager> authManagerProvider;
    @Mock private ObjectProvider<PasswordEncoder> passwordEncoderProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setup() {
        authorizationService = new AuthorizationService(
                userRepository,
                authMapper,
                tokenService,
                mailService,
                authManagerProvider,
                passwordEncoderProvider
        );

        lenient().when(authManagerProvider.getObject()).thenReturn(authenticationManager);
        lenient().when(passwordEncoderProvider.getObject()).thenReturn(passwordEncoder);
    }

    @Nested
    @DisplayName("Testes de Registro de Usuário")
    class RegisterTests {

        @Test
        void registerSuccess() {
            RegisterReq req = new RegisterReq();
            req.setEmail("test@fitoherb.com");
            req.setName("Test User");

            User userEntity = new User();
            userEntity.setEmail(req.getEmail());

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
            when(authMapper.registerReqToEntity(req)).thenReturn(userEntity);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_pwd");
            when(userRepository.save(any(User.class))).thenReturn(userEntity);

            User result = authorizationService.register(req);

            assertNotNull(result);
            verify(userRepository, times(1)).save(any(User.class));
            verify(mailService, times(1)).sendEmail(any());
            assertEquals("hashed_pwd", userEntity.getPassword());
        }

        @Test
        void registerEmailExists() {
            RegisterReq req = new RegisterReq();
            req.setEmail("exists@fitoherb.com");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(new User()));

            assertThrows(ResourceAlreadyExistsException.class, () -> authorizationService.register(req));
            verify(userRepository, never()).save(any());
        }

        @Test
        void registerDatabaseError() {
            RegisterReq req = new RegisterReq();
            req.setEmail("db-error@fitoherb.com");

            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
            when(authMapper.registerReqToEntity(any())).thenReturn(new User());
            when(userRepository.save(any())).thenThrow(new RuntimeException());

            assertThrows(DatabaseOperationException.class, () -> authorizationService.register(req));
        }
    }

    @Nested
    @DisplayName("Testes de Login e Autenticação")
    class AuthTests {

        @Test
        void loginSuccess() {
            LoginReq req = new LoginReq("user@fitoherb.com", "password");
            User user = new User();
            user.setEmail(req.getEmail());

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(user);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
            when(tokenService.generateToken(user)).thenReturn("jwt_token");

            String token = authorizationService.login(req);

            assertEquals("jwt_token", token);
            verify(tokenService).generateToken(user);
        }

        @Test
        void loadUserByUsernameSuccess() {
            String email = "find@fitoherb.com";
            User user = new User();
            user.setEmail(email);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            UserDetails result = authorizationService.loadUserByUsername(email);

            assertNotNull(result);
            assertEquals(email, result.getUsername());
        }

        @Test
        void loadUserByUsernameNotFound() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            assertThrows(UsernameNotFoundException.class, () -> authorizationService.loadUserByUsername("none@test.com"));
        }
    }

    @Nested
    @DisplayName("Testes de Contexto de Segurança")
    class SecurityContextTests {

        @BeforeEach
        void setContext() {
            SecurityContextHolder.setContext(securityContext);
        }

        @Test
        void isAuthenticatedTrue() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            assertTrue(authorizationService.isAuthenticated());
        }

        @Test
        void isAuthenticatedFalseForAnonymous() {
            AnonymousAuthenticationToken anon = mock(AnonymousAuthenticationToken.class);
            when(securityContext.getAuthentication()).thenReturn(anon);
            assertFalse(authorizationService.isAuthenticated());
        }

        @Test
        void isAdminTrue() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            doReturn(Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .when(authentication).getAuthorities();
            assertTrue(authorizationService.isAdmin());
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Robustez")
    class SecurityVulnerabilityTests {

        @Test
        void sqlInjectionAttempt() {
            RegisterReq req = new RegisterReq();
            req.setEmail("admin@test.com' OR 1=1; --");
            req.setName("Hacker");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(authMapper.registerReqToEntity(req)).thenReturn(new User());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(new User());

            assertDoesNotThrow(() -> authorizationService.register(req));
        }

        @Test
        void xssAttempt() {
            RegisterReq req = new RegisterReq();
            req.setName("<script>alert('XSS')</script>");
            req.setEmail("victim@test.com");

            User userEntity = new User();
            userEntity.setEmail(req.getEmail());
            when(authMapper.registerReqToEntity(req)).thenReturn(userEntity);
            when(userRepository.save(any())).thenReturn(userEntity);

            assertDoesNotThrow(() -> authorizationService.register(req));
        }

        @Test
        void nullByteInjection() {
            RegisterReq req = new RegisterReq();
            req.setEmail("test@test.com\0.exe");
            req.setName("Malicious\0User");

            User userEntity = new User();
            userEntity.setEmail(req.getEmail());
            when(authMapper.registerReqToEntity(req)).thenReturn(userEntity);
            when(userRepository.save(any())).thenReturn(userEntity);

            assertDoesNotThrow(() -> authorizationService.register(req));
        }

        @Test
        void extremePayloadSize() {
            RegisterReq req = new RegisterReq();
            req.setName("A".repeat(5000));
            req.setEmail("long@test.com");

            User userEntity = new User();
            userEntity.setEmail(req.getEmail());
            when(authMapper.registerReqToEntity(req)).thenReturn(userEntity);
            when(userRepository.save(any())).thenReturn(userEntity);

            assertDoesNotThrow(() -> authorizationService.register(req));
        }

        @Test
        void passwordEntropy() {
            for (int i = 0; i < 100; i++) {
                RegisterReq req = new RegisterReq();
                req.setEmail("user" + i + "@test.com");
                User userEntity = new User();
                userEntity.setEmail(req.getEmail());
                when(authMapper.registerReqToEntity(req)).thenReturn(userEntity);
                when(userRepository.save(any())).thenReturn(userEntity);
                authorizationService.register(req);
            }
            verify(passwordEncoder, times(100)).encode(anyString());
        }
    }
}