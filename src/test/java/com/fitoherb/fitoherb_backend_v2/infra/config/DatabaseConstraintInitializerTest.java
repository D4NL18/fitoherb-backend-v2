package com.fitoherb.fitoherb_backend_v2.infra.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseConstraintInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseConstraintInitializer initializer;

    @Test
    @DisplayName("Deve executar os comandos SQL para atualizar constraint users_role_check com sucesso")
    void shouldExecuteConstraintUpdateSuccessfully() {
        assertDoesNotThrow(() -> initializer.run(new DefaultApplicationArguments()));

        verify(jdbcTemplate).execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        verify(jdbcTemplate).execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role >= 0 AND role <= 2)");
    }

    @Test
    @DisplayName("Não deve lançar exceção se comando SQL falhar")
    void shouldHandleExceptionGracefully() {
        doThrow(new RuntimeException("DB offline")).when(jdbcTemplate).execute(anyString());

        assertDoesNotThrow(() -> initializer.run(new DefaultApplicationArguments()));
    }
}
