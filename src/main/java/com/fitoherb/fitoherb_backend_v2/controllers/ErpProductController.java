package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.DailySyncReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.IgnoreCurationProductReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.SuggestVariantsReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.*;
import com.fitoherb.fitoherb_backend_v2.services.ErpCurationService;
import com.fitoherb.fitoherb_backend_v2.services.ErpDailySyncService;
import com.fitoherb.fitoherb_backend_v2.services.ErpProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_SLUG_INVALID;
import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.SLUG_REGEX;

@RestController
@RequiredArgsConstructor
@RequestMapping("/erp-products")
@Tag(name = "ERP Products & Sync", description = "Integração somente-leitura com o catálogo do ERP Atrio, sugestão de variações, sincronização diária e curadoria rápida.")
public class ErpProductController {

    private final ErpProductService erpProductService;
    private final ErpDailySyncService erpDailySyncService;
    private final ErpCurationService erpCurationService;

    @Operation(summary = "Pesquisar catálogo do ERP Atrio em memória RAM / SQL Server")
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @GetMapping("/search")
    public ResponseEntity<ApiEnvelope<List<ErpCatalogItemRes>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String supplierSlug,
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(required = false) String excludeProductSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validateSearchParams(q, size);
        ApiEnvelope<List<ErpCatalogItemRes>> response = erpProductService.search(
                q, supplierSlug, includeInactive, excludeProductSlug, page, size
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consultar item unitário do ERP Atrio pelo código Id_Pro")
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @GetMapping("/{erpCode}")
    public ResponseEntity<ApiEnvelope<ErpCatalogItemRes>> getByErpCode(
            @PathVariable @Positive Long erpCode,
            @RequestParam(required = false) String excludeProductSlug
    ) {
        return ResponseEntity.ok(erpProductService.getByErpCode(erpCode, excludeProductSlug));
    }

    @Operation(summary = "Sugerir variações e topologia em lote a partir de códigos ERP")
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @PostMapping({"/suggest-variants", "/suggestions"})
    public ResponseEntity<ApiEnvelope<SuggestVariantsRes>> suggestVariants(
            @RequestBody @Valid SuggestVariantsReq req
    ) {
        return ResponseEntity.ok(erpProductService.suggestVariants(req));
    }

    @Operation(summary = "Obter status atual da sincronização diária ERP")
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @GetMapping("/sync/status")
    public ResponseEntity<ApiEnvelope<ErpSyncStatusRes>> getSyncStatus() {
        return ResponseEntity.ok(erpProductService.getSyncStatus());
    }

    @Operation(summary = "Listar produtos ativos aguardando imagem na Central de Curadoria Rápida")
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @GetMapping("/curation-pending")
    public ResponseEntity<ApiEnvelope<List<QuickCurationPendingItemRes>>> getCurationPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean onlyFallback,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(erpCurationService.getCurationPending(page, size, onlyFallback, search));
    }

    @Operation(summary = "Descartar e bloquear ressincronização automática de produto pendente de curadoria")
    @PreAuthorize("@authorizationService.isAdminOrUser()")
    @PostMapping("/curation-pending/{slug}/ignore")
    public ResponseEntity<ApiEnvelope<IgnoreCurationProductRes>> ignoreCurationPending(
            @PathVariable @NotBlank @Pattern(regexp = SLUG_REGEX, message = MSG_SLUG_INVALID) String slug,
            @RequestBody(required = false) @Valid IgnoreCurationProductReq req
    ) {
        String reason = req != null ? req.getReason() : null;
        String actorEmail = resolveCurrentActor();
        IgnoreCurationProductRes res = erpCurationService.ignorePendingProduct(slug, reason, actorEmail);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", OffsetDateTime.now().toString());
        meta.put("aiCatalogRefreshed", true);

        return ResponseEntity.ok(new ApiEnvelope<>(res, meta));
    }

    @Operation(summary = "Executar ciclo diário idempotente de sincronização ERP -> Site")
    @PreAuthorize("hasRole('SYNC_SCHEDULER') or @authorizationService.isAdmin()")
    @PostMapping("/sync/daily")
    public ResponseEntity<ApiEnvelope<DailySyncRes>> triggerDailySync(
            @RequestParam(defaultValue = "FAIL") String ifRunning,
            @RequestBody(required = false) DailySyncReq req
    ) {
        boolean acceptIfRunning = "ACCEPT".equalsIgnoreCase(ifRunning);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isScheduler = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SYNC_SCHEDULER".equals(a.getAuthority()));

        String triggerType = isScheduler ? "CLOUD_SCHEDULER" : "MANUAL_ADMIN";
        String triggeredBy = isScheduler
                ? "CLOUD_SCHEDULER"
                : (auth != null && auth.getName() != null ? auth.getName() : "ADMIN");

        long startMs = System.currentTimeMillis();
        DailySyncRes res = erpDailySyncService.executeSync(
                req != null ? req : new DailySyncReq(),
                triggerType,
                triggeredBy,
                acceptIfRunning
        );
        long durationMs = System.currentTimeMillis() - startMs;

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", OffsetDateTime.now().toString());
        meta.put("durationMs", durationMs);
        meta.put("triggerSource", triggerType);

        HttpStatus status = "ALREADY_RUNNING".equals(res.getStatus()) ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(new ApiEnvelope<>(res, meta));
    }

    @Operation(summary = "Forçar atualização imediata do snapshot ERP em memória RAM")
    @PreAuthorize("@authorizationService.isAdmin()")
    @PostMapping("/sync")
    public ResponseEntity<ApiEnvelope<SyncErpCatalogRes>> syncCatalogCache() {
        return ResponseEntity.ok(erpProductService.syncCatalogCache());
    }

    private void validateSearchParams(String q, int size) {
        if (q == null || q.trim().isEmpty()) {
            throw new IllegalArgumentException("O parâmetro de busca 'q' é obrigatório.");
        }
        String trimmed = q.trim();
        if (!trimmed.matches("\\d+") && trimmed.length() < 2) {
            throw new IllegalArgumentException("O termo de busca textual deve possuir pelo menos 2 caracteres.");
        }
        if (size <= 0 || size > 50) {
            throw new IllegalArgumentException("O tamanho da página deve estar entre 1 e 50.");
        }
    }

    private String resolveCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
