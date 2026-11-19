package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.entities.ApiKey;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.repositories.ApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository repository;

    @InjectMocks
    private ApiKeyService service;

    @Test
    @DisplayName("create: persiste apenas o hash e devolve o segredo uma unica vez")
    void createStoresOnlyHash() {
        when(repository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

        var created = service.create("claude-projeto-x");

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(repository).save(captor.capture());
        ApiKey saved = captor.getValue();

        assertTrue(created.rawKey().startsWith("fh_"));
        assertNotEquals(created.rawKey(), saved.getKeyHash());
        assertEquals(ApiKeyService.hash(created.rawKey()), saved.getKeyHash());
        assertEquals(64, saved.getKeyHash().length());
        assertTrue(created.rawKey().startsWith(saved.getKeyPrefix()));
        assertEquals("claude-projeto-x", saved.getName());
    }

    @Test
    @DisplayName("create: chaves geradas sao distintas")
    void createGeneratesUniqueKeys() {
        when(repository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

        assertNotEquals(service.create("a-key").rawKey(), service.create("b-key").rawKey());
    }

    @Test
    @DisplayName("authenticate: chave valida e ativa retorna a entidade")
    void authenticateValid() {
        ApiKey key = new ApiKey();
        key.setName("k");
        when(repository.findByKeyHashAndRevokedAtIsNull(ApiKeyService.hash("fh_abc"))).thenReturn(Optional.of(key));
        when(repository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

        Optional<ApiKey> result = service.authenticate("fh_abc");

        assertTrue(result.isPresent());
        assertNotNull(result.get().getLastUsedAt());
    }

    @Test
    @DisplayName("authenticate: nao regrava lastUsedAt quando usado recentemente")
    void authenticateThrottlesWrites() {
        ApiKey key = new ApiKey();
        key.setLastUsedAt(LocalDateTime.now().minusSeconds(10));
        when(repository.findByKeyHashAndRevokedAtIsNull(any())).thenReturn(Optional.of(key));

        assertTrue(service.authenticate("fh_abc").isPresent());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("authenticate: chave desconhecida ou revogada retorna vazio")
    void authenticateUnknown() {
        when(repository.findByKeyHashAndRevokedAtIsNull(any())).thenReturn(Optional.empty());

        assertTrue(service.authenticate("fh_nope").isEmpty());
    }

    @Test
    @DisplayName("authenticate: valor nulo ou em branco nunca consulta o banco")
    void authenticateBlank() {
        assertTrue(service.authenticate(null).isEmpty());
        assertTrue(service.authenticate("  ").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("revoke: marca revokedAt")
    void revokeSetsTimestamp() {
        ApiKey key = new ApiKey();
        when(repository.findById("id1")).thenReturn(Optional.of(key));

        service.revoke("id1");

        assertNotNull(key.getRevokedAt());
        assertFalse(key.isActive());
        verify(repository).save(key);
    }

    @Test
    @DisplayName("revoke: id inexistente lanca ResourceNotFoundException")
    void revokeNotFound() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.revoke("x"));
    }
}
