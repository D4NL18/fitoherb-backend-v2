package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ErpCatalogCacheService {

    private record CachedCatalog(
            List<ErpCatalogItemSnapshot> items,
            Map<Long, ErpCatalogItemSnapshot> byCode,
            Instant loadedAt
    ) {}

    private final ErpCatalogGateway erpCatalogGateway;
    private final long ttlSeconds;
    private final Clock clock;
    private final AtomicReference<CachedCatalog> cacheRef = new AtomicReference<>();

    @Autowired
    public ErpCatalogCacheService(
            ErpCatalogGateway erpCatalogGateway,
            @Value("${app.erp.cache.ttl-seconds:900}") long ttlSeconds
    ) {
        this(erpCatalogGateway, ttlSeconds, Clock.systemUTC());
    }

    public ErpCatalogCacheService(ErpCatalogGateway erpCatalogGateway, long ttlSeconds, Clock clock) {
        this.erpCatalogGateway = erpCatalogGateway;
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
    }

    public Optional<ErpCatalogItemSnapshot> findByErpCode(Long erpCode) {
        if (erpCode == null) {
            return Optional.empty();
        }
        CachedCatalog snapshot = ensureFreshSnapshot();
        return Optional.ofNullable(snapshot.byCode().get(erpCode));
    }

    public List<ErpCatalogItemSnapshot> getCatalogSnapshot() {
        return ensureFreshSnapshot().items();
    }

    public Instant getLastSyncedAt() {
        CachedCatalog current = cacheRef.get();
        return current != null ? current.loadedAt() : null;
    }

    public synchronized List<ErpCatalogItemSnapshot> forceRefresh() {
        List<ErpCatalogItemSnapshot> fetched = erpCatalogGateway.fetchFullCatalog();
        Map<Long, ErpCatalogItemSnapshot> map = new LinkedHashMap<>();
        if (fetched != null) {
            for (ErpCatalogItemSnapshot item : fetched) {
                if (item != null && item.getErpCode() != null) {
                    map.put(item.getErpCode(), item);
                }
            }
        }
        List<ErpCatalogItemSnapshot> immutableList = List.copyOf(map.values());
        CachedCatalog updated = new CachedCatalog(immutableList, Collections.unmodifiableMap(map), clock.instant());
        cacheRef.set(updated);
        return immutableList;
    }

    private CachedCatalog ensureFreshSnapshot() {
        CachedCatalog current = cacheRef.get();
        Instant now = clock.instant();
        if (current != null && now.isBefore(current.loadedAt().plusSeconds(ttlSeconds))) {
            return current;
        }
        synchronized (this) {
            current = cacheRef.get();
            if (current != null && now.isBefore(current.loadedAt().plusSeconds(ttlSeconds))) {
                return current;
            }
            forceRefresh();
            return cacheRef.get();
        }
    }
}
