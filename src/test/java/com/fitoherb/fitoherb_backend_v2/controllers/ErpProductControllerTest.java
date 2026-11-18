package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.*;
import com.fitoherb.fitoherb_backend_v2.enums.DailySyncMode;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestExceptionHandler;
import com.fitoherb.fitoherb_backend_v2.infra.security.SecurityConfigurations;
import com.fitoherb.fitoherb_backend_v2.infra.security.SecurityFilter;
import com.fitoherb.fitoherb_backend_v2.infra.security.SyncSchedulerAuthFilter;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import com.fitoherb.fitoherb_backend_v2.services.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ErpProductController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class, SyncSchedulerAuthFilter.class, RestExceptionHandler.class})
@TestPropertySource(properties = {
        "app.erp.sync.scheduler-token=test-scheduler-secret-token"
})
class ErpProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ErpProductService erpProductService;
    @MockitoBean private ErpDailySyncService erpDailySyncService;
    @MockitoBean private ErpCurationService erpCurationService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean(name = "authorizationService") private AuthorizationService authorizationService;

    @Nested
    @DisplayName("GET /erp-products/search")
    class SearchTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("P-407: ROLE_USER deve buscar catálogo ERP com envelope data/meta")
        void searchAllowedForUser() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);
            ErpCatalogItemRes item = ErpCatalogItemRes.builder()
                    .erpCode(131476L)
                    .erpDescription("100% PURE WHEY 900G BAUNILHA")
                    .inativoPro(false)
                    .inExterno01(true)
                    .alreadyMapped(false)
                    .build();
            when(erpProductService.search(eq("whey"), isNull(), eq(true), isNull(), eq(0), eq(20)))
                    .thenReturn(new ApiEnvelope<>(List.of(item), Map.of("totalElements", 1)));

            mockMvc.perform(get("/erp-products/search").param("q", "whey"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].erpCode").value(131476))
                    .andExpect(jsonPath("$.meta.totalElements").value(1));
        }

        @Test
        @WithMockUser(roles = "SELLER")
        @DisplayName("P-408: ROLE_SELLER deve receber 403 Forbidden")
        void searchForbiddenForSeller() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(false);

            mockMvc.perform(get("/erp-products/search").param("q", "whey"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("P-408: Usuário anônimo não autenticado não pode acessar busca ERP")
        void searchRejectedForAnonymous() throws Exception {
            mockMvc.perform(get("/erp-products/search").param("q", "whey"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("P-407: Deve retornar 400 Bad Request para termo q vazio ou 1 letra não numérica")
        void searchRejectsShortNonNumericTerm() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);

            mockMvc.perform(get("/erp-products/search").param("q", "a"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /erp-products/curation-pending & POST /erp-products/curation-pending/{slug}/ignore")
    class CurationTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("P-413: ROLE_USER e ROLE_ADMIN podem listar pendências de curadoria rápida")
        void curationPendingAllowedForUser() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);
            QuickCurationPendingItemRes item = QuickCurationPendingItemRes.builder()
                    .slug("creatina-turbo-black-skull")
                    .name("Creatina Turbo")
                    .active(true)
                    .curationStatus(ProductCurationStatus.PENDING_IMAGE)
                    .build();
            when(erpCurationService.getCurationPending(0, 20, null, null))
                    .thenReturn(new ApiEnvelope<>(List.of(item), Map.of("pendingCount", 1)));

            mockMvc.perform(get("/erp-products/curation-pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].slug").value("creatina-turbo-black-skull"))
                    .andExpect(jsonPath("$.data[0].curationStatus").value("PENDING_IMAGE"));
        }

        @Test
        @WithMockUser(roles = "SELLER")
        @DisplayName("P-413: ROLE_SELLER deve receber 403 Forbidden em curation-pending")
        void curationPendingForbiddenForSeller() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(false);

            mockMvc.perform(get("/erp-products/curation-pending"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("P-413: POST /erp-products/curation-pending/{slug}/ignore deve validar SLUG_REGEX")
        void ignoreRejectsInvalidSlugPattern() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);

            mockMvc.perform(post("/erp-products/curation-pending/INVALID_SLUG!/ignore")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Brinde\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("P-413: POST /erp-products/curation-pending/{slug}/ignore deve ocultar e gravar IGNORED")
        void ignoreSuccessForValidSlug() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);
            IgnoreCurationProductRes res = IgnoreCurationProductRes.builder()
                    .slug("squeeze-evento-fitoherb")
                    .active(false)
                    .curationStatus(ProductCurationStatus.IGNORED)
                    .ignoredErpCodes(List.of(990999L))
                    .build();
            when(erpCurationService.ignorePendingProduct(eq("squeeze-evento-fitoherb"), eq("Brinde"), anyString()))
                    .thenReturn(res);

            mockMvc.perform(post("/erp-products/curation-pending/squeeze-evento-fitoherb/ignore")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Brinde\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.slug").value("squeeze-evento-fitoherb"))
                    .andExpect(jsonPath("$.data.active").value(false))
                    .andExpect(jsonPath("$.data.curationStatus").value("IGNORED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("P-413: POST /erp-products/curation-pending/{slug}/ignore deve retornar 404 para slug inexistente")
        void ignoreReturns404WhenNotFound() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);
            when(erpCurationService.ignorePendingProduct(eq("nao-existe"), isNull(), anyString()))
                    .thenThrow(new ResourceNotFoundException("Produto não encontrado"));

            mockMvc.perform(post("/erp-products/curation-pending/nao-existe/ignore"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /erp-products/sync/daily & POST /erp-products/sync")
    class SyncTests {

        @Test
        @DisplayName("P-410: Deve aceitar disparo M2M com header X-Sync-Scheduler-Token válido")
        void dailySyncAllowedWithValidSchedulerHeader() throws Exception {
            DailySyncRes res = DailySyncRes.builder()
                    .runId("run-1")
                    .mode(DailySyncMode.INCREMENTAL)
                    .status("COMPLETED")
                    .dryRun(false)
                    .aiCatalogRefreshed(true)
                    .build();
            when(erpDailySyncService.executeSync(any(), eq("CLOUD_SCHEDULER"), eq("CLOUD_SCHEDULER"), eq(false)))
                    .thenReturn(res);

            mockMvc.perform(post("/erp-products/sync/daily")
                            .header("X-Sync-Scheduler-Token", "test-scheduler-secret-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("P-410: Deve rejeitar header X-Sync-Scheduler-Token inválido com 401")
        void dailySyncRejectedWithInvalidSchedulerHeader() throws Exception {
            mockMvc.perform(post("/erp-products/sync/daily")
                            .header("X-Sync-Scheduler-Token", "token-errado")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("P-410: Deve bloquear disparo anônimo sem header nem JWT")
        void dailySyncRejectedForAnonymous() throws Exception {
            mockMvc.perform(post("/erp-products/sync/daily")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("P-410: ROLE_USER sem token scheduler deve receber 403 em /sync/daily")
        void dailySyncForbiddenForUserRole() throws Exception {
            when(authorizationService.isAdmin()).thenReturn(false);

            mockMvc.perform(post("/erp-products/sync/daily")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("P-410: ROLE_ADMIN deve poder disparar /sync/daily e retornar 202 quando ifRunning=ACCEPT e ALREADY_RUNNING")
        void dailySyncAcceptsAlreadyRunningWhenIfRunningAccept() throws Exception {
            when(authorizationService.isAdmin()).thenReturn(true);
            DailySyncRes res = DailySyncRes.builder()
                    .runId("run-active")
                    .mode(DailySyncMode.INCREMENTAL)
                    .status("ALREADY_RUNNING")
                    .build();
            when(erpDailySyncService.executeSync(any(), eq("MANUAL_ADMIN"), anyString(), eq(true)))
                    .thenReturn(res);

            mockMvc.perform(post("/erp-products/sync/daily")
                            .param("ifRunning", "ACCEPT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.status").value("ALREADY_RUNNING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("P-410: Deve retornar 409 Conflict quando ifRunning=FAIL e já houver execução em andamento")
        void dailySyncReturns409WhenAlreadyRunningAndFailMode() throws Exception {
            when(authorizationService.isAdmin()).thenReturn(true);
            when(erpDailySyncService.executeSync(any(), eq("MANUAL_ADMIN"), anyString(), eq(false)))
                    .thenThrow(new ResourceAlreadyExistsException("Já existe uma sincronização diária em andamento."));

            mockMvc.perform(post("/erp-products/sync/daily")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /erp-products/sync é restrito a ROLE_ADMIN (ROLE_USER recebe 403)")
        void syncCacheForbiddenForUserRole() throws Exception {
            when(authorizationService.isAdmin()).thenReturn(false);

            mockMvc.perform(post("/erp-products/sync"))
                    .andExpect(status().isForbidden());
        }
    }
}
