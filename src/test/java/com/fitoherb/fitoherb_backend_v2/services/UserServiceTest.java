package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.PasswordUpdateReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.UserReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.UserRes;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.UserMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User userEntity;
    private UserRes userRes;

    @BeforeEach
    void setup() {
        userEntity = new User();
        userEntity.setEmail("daniel@fitoherb.com");
        userEntity.setName("Daniel Marinho");

        userRes = new UserRes();
    }

    @Nested
    @DisplayName("Testes de Busca")
    class FindTests {

        @Test
        void getUserByEmailSuccess() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));
            when(userMapper.entityToRes(userEntity)).thenReturn(userRes);

            UserRes result = userService.getUserByEmail("daniel@fitoherb.com");

            assertNotNull(result);
            verify(userRepository).findByEmail("daniel@fitoherb.com");
        }

        @Test
        void getUserByEmailNotFound() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("nao-existe@test.com"));
        }

        @Test
        void getAllUsersPaginatedSuccess() {
            Page<User> page = new PageImpl<>(List.of(userEntity));
            when(userRepository.findAllFiltered(anyString(), any(Pageable.class))).thenReturn(page);
            when(userMapper.entityToRes(any())).thenReturn(userRes);

            Page<UserRes> result = userService.getAllUsers("search", 0, "name", "ASC");

            assertEquals(1, result.getTotalElements());
            verify(userRepository).findAllFiltered(eq("search"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Testes de Atualização")
    class UpdateTests {

        @Test
        void updateUserByEmailSuccess() {
            UserReq req = new UserReq();
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any(User.class))).thenReturn(userEntity);

            assertDoesNotThrow(() -> userService.updateUserByEmail("daniel@fitoherb.com", req));
            verify(userRepository).save(userEntity);
        }

        @Test
        void updatePasswordByEmailSuccess() {
            PasswordUpdateReq req = new PasswordUpdateReq();
            req.setPassword("new-password");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(userEntity);

            userService.updatePasswordByEmail("daniel@fitoherb.com", req);

            assertEquals("encoded-password", userEntity.getPassword());
            verify(userRepository).save(userEntity);
        }

        @Test
        void updatePasswordDatabaseError() {
            PasswordUpdateReq req = new PasswordUpdateReq();
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException());

            assertThrows(DatabaseOperationException.class, () -> userService.updatePasswordByEmail("email", req));
        }
    }

    @Nested
    @DisplayName("Testes de Deleção")
    class DeleteTests {

        @Test
        void deleteUserByEmailSuccess() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));

            userService.deleteUserByEmail("daniel@fitoherb.com");

            verify(userRepository).delete(any(User.class));
        }

        @Test
        void deleteUserDatabaseError() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));
            doThrow(new RuntimeException()).when(userRepository).delete(any(User.class));

            assertThrows(DatabaseOperationException.class, () -> userService.deleteUserByEmail("email"));
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Robustez")
    class SecurityTests {

        @Test
        void sqlInjectionOnUserSearch() {
            Page<User> emptyPage = new PageImpl<>(Collections.emptyList());
            String malicious = "'; DROP TABLE users; --";
            when(userRepository.findAllFiltered(eq(malicious), any(Pageable.class))).thenReturn(emptyPage);

            assertDoesNotThrow(() -> userService.getAllUsers(malicious, 0, "name", "ASC"));
        }

        @Test
        void xssOnUserEmail() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            String xssEmail = "<script>alert('xss')</script>@test.com";

            assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail(xssEmail));
        }

        @Test
        void nullByteInEmail() {
            String malicious = "daniel@test.com\0";
            when(userRepository.findByEmail(malicious)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail(malicious));
        }

        @Test
        void extremePayloadSizeInUpdate() {
            String hugeEmail = "a".repeat(2000) + "@test.com";
            when(userRepository.findByEmail(hugeEmail)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> userService.deleteUserByEmail(hugeEmail));
        }
    }
}