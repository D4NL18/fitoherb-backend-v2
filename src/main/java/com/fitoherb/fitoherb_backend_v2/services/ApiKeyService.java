package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.entities.ApiKey;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.repositories.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Gerencia chaves de API administrativas. A chave tem 256 bits de entropia, portanto um
 * SHA-256 sem salt e suficiente para armazenamento (nao e uma senha escolhida por humano).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    public static final String KEY_PREFIX = "fh_";
    private static final int PREFIX_LENGTH = 8;
    private static final int KEY_BYTES = 32;
    private static final long LAST_USED_THROTTLE_SECONDS = 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;

    public record CreatedApiKey(ApiKey entity, String rawKey) {
    }

    @Transactional
    public CreatedApiKey create(String name) {
        byte[] bytes = new byte[KEY_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String rawKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        ApiKey apiKey = new ApiKey();
        apiKey.setName(name.trim());
        apiKey.setKeyHash(hash(rawKey));
        apiKey.setKeyPrefix(rawKey.substring(0, PREFIX_LENGTH));

        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("API key created: name={}, prefix={}", saved.getName(), saved.getKeyPrefix());
        return new CreatedApiKey(saved, rawKey);
    }

    @Transactional
    public Optional<ApiKey> authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }

        Optional<ApiKey> found = apiKeyRepository.findByKeyHashAndRevokedAtIsNull(hash(rawKey.trim()));
        found.ifPresent(this::touchLastUsed);
        return found;
    }

    @Transactional(readOnly = true)
    public List<ApiKey> findAll() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void revoke(String id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key nao encontrada"));
        if (apiKey.isActive()) {
            apiKey.setRevokedAt(LocalDateTime.now());
            apiKeyRepository.save(apiKey);
            log.info("API key revoked: name={}, prefix={}", apiKey.getName(), apiKey.getKeyPrefix());
        }
    }

    private void touchLastUsed(ApiKey apiKey) {
        LocalDateTime last = apiKey.getLastUsedAt();
        LocalDateTime now = LocalDateTime.now();
        if (last == null || last.isBefore(now.minusSeconds(LAST_USED_THROTTLE_SECONDS))) {
            apiKey.setLastUsedAt(now);
            apiKeyRepository.save(apiKey);
        }
    }

    public static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
