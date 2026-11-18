package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductCategoryRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ProductRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SupplierRes;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.UnprocessableEntityException;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestExceptionHandler;
import com.fitoherb.fitoherb_backend_v2.infra.security.SecurityConfigurations;
import com.fitoherb.fitoherb_backend_v2.infra.security.SecurityFilter;
import com.fitoherb.fitoherb_backend_v2.infra.security.SyncSchedulerAuthFilter;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import com.fitoherb.fitoherb_backend_v2.services.AuthorizationService;
import com.fitoherb.fitoherb_backend_v2.services.ProductService;
import com.fitoherb.fitoherb_backend_v2.services.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class, SyncSchedulerAuthFilter.class, RestExceptionHandler.class})
class ProductControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ProductService productService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean(name = "authorizationService") private AuthorizationService authorizationService;

    @Nested
    @DisplayName("POST /products & PUT /products/{slug}: Imagem Opcional em PENDING_IMAGE e Semântica HTTP")
    class CreateAndUpdateMultipartTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("P-412: POST /products deve aceitar requisição sem part 'image' quando curationStatus == PENDING_IMAGE")
        void createProductWithoutImageAllowedWhenPendingImage() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);
            Product saved = new Product();
            saved.setId("prod-new-1");
            saved.setCurationStatus(ProductCurationStatus.PENDING_IMAGE);
            when(productService.createProduct(any(), isNull())).thenReturn(saved);

            MockMultipartFile productJson = new MockMultipartFile(
                    "product",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    """
                    {
                      "name": "Creatina Turbo 300g",
                      "categorySlug": "suplementos",
                      "supplierSlug": "black-skull",
                      "curationStatus": "PENDING_IMAGE",
                      "skus": [{ "erpCode": 991050, "flavour": null, "presentation": null }]
                    }
                    """.getBytes(StandardCharsets.UTF_8)
            );

            mockMvc.perform(multipart("/products").file(productJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/products/prod-new-1")));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("IMG-001: POST /products sem 'image' com curationStatus COMPLETE deve retornar 400 Bad Request")
        void createProductWithoutImageRejectedWhenComplete() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);
            when(productService.createProduct(any(), isNull()))
                    .thenThrow(new IllegalArgumentException("A imagem do produto é obrigatória."));

            MockMultipartFile productJson = new MockMultipartFile(
                    "product",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    """
                    {
                      "name": "Mel Silvestre 500g",
                      "categorySlug": "naturais",
                      "supplierSlug": "apisnutri",
                      "curationStatus": "COMPLETE",
                      "skus": [{ "erpCode": 41025, "flavour": null, "presentation": null }]
                    }
                    """.getBytes(StandardCharsets.UTF_8)
            );

            mockMvc.perform(multipart("/products").file(productJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("P-406: PUT /products/{slug} propagando 422 ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED com details.inactiveErpCodes")
        void updateProductReturns422WithCodeAndDetails() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);
            doThrow(new UnprocessableEntityException(
                    "ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED",
                    "O código ERP '131479' está marcado como inativo no ERP Atrio.",
                    Map.of("inactiveErpCodes", List.of(131479L))
            )).when(productService).updateProductBySlug(any(), any(), eq("whey-900g"));

            MockMultipartFile productJson = new MockMultipartFile(
                    "product",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    """
                    {
                      "name": "100% Pure Whey 900g",
                      "categorySlug": "proteinas",
                      "supplierSlug": "max-titanium",
                      "allowInactiveErp": false,
                      "skus": [{ "erpCode": 131479, "flavour": "Cookies", "presentation": null }]
                    }
                    """.getBytes(StandardCharsets.UTF_8)
            );

            mockMvc.perform(multipart("/products/whey-900g")
                            .file(productJson)
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED"))
                    .andExpect(jsonPath("$.details.inactiveErpCodes[0]").value(131479));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("P-404: PUT /products/{slug} propagando 409 Conflict específico de código ERP sem mascarar como 500")
        void updateProductReturns409OnErpCodeConflict() throws Exception {
            when(authorizationService.isAdminOrUser()).thenReturn(true);
            doThrow(new ResourceAlreadyExistsException(
                    "O código ERP '131476' já está vinculado ao produto '100% Pure Whey 900g'."
            )).when(productService).updateProductBySlug(any(), any(), eq("outro-whey"));

            MockMultipartFile productJson = new MockMultipartFile(
                    "product",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    """
                    {
                      "name": "Outro Whey",
                      "categorySlug": "proteinas",
                      "supplierSlug": "max-titanium",
                      "skus": [{ "erpCode": 131476, "flavour": "Baunilha", "presentation": null }]
                    }
                    """.getBytes(StandardCharsets.UTF_8)
            );

            mockMvc.perform(multipart("/products/outro-whey")
                            .file(productJson)
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("131476")));
        }
    }

    @Nested
    @DisplayName("GET /products/gallery: Segregação Pública (P-408 & P-411)")
    class PublicGallerySecurityContractTests {

        @Test
        @DisplayName("P-408 & C-14: Vitrine pública deve responder 200 sem auth e jamais expor campos ERP ou auditoria")
        void galleryNeverExposesErpOrAuditFields() throws Exception {
            ProductRes publicRes = new ProductRes(
                    "B.O.P.E",
                    null,
                    "Pré-treino",
                    List.of("Limão", "Melancia"),
                    List.of("150g", "300g"),
                    "bope-black-skull",
                    new ProductCategoryRes(),
                    new SupplierRes(),
                    "18-11-2026 10:00:00"
            );
            when(productService.getProductGallery(isNull(), isNull(), isNull(), eq(0), eq(15), eq("ASC")))
                    .thenReturn(new PageImpl<>(List.of(publicRes)));

            mockMvc.perform(get("/products/gallery"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].slug").value("bope-black-skull"))
                    .andExpect(content().string(not(containsString("erpCode"))))
                    .andExpect(content().string(not(containsString("skus"))))
                    .andExpect(content().string(not(containsString("erpInactive"))))
                    .andExpect(content().string(not(containsString("curationStatus"))))
                    .andExpect(content().string(not(containsString("createdBy"))))
                    .andExpect(content().string(not(containsString("Estoque"))));
        }
    }
}
