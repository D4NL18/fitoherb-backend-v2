package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductSkuReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ServiceUnavailableException;
import com.fitoherb.fitoherb_backend_v2.exceptions.UnprocessableEntityException;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpCatalogValidationServiceTest {

    @Mock private ErpCatalogCacheService erpCatalogCacheService;
    @Mock private ProductSkuRepository productSkuRepository;
    @Spy private ProductVariantSanitizer variantSanitizer = new ProductVariantSanitizer();
    @Spy private ProductTopologyValidator topologyValidator = new ProductTopologyValidator();

    @InjectMocks
    private ErpCatalogValidationService validationService;

    private ProductReq req;

    @BeforeEach
    void setup() {
        req = new ProductReq();
        req.setName("100% Pure Whey 900g");
        req.setCategorySlug("suplementos");
        req.setSupplierSlug("max-titanium");
        req.setAllowInactiveErp(false);
    }

    @Test
    @DisplayName("P-402: Deve rejeitar erpCode nulo ou <= 0")
    void shouldRejectNonPositiveOrNullErpCode() {
        req.setSkus(List.of(new ProductSkuReq(0L, null, null)));
        assertThrows(IllegalArgumentException.class, () -> validationService.validateForProduct(req, null));

        req.setSkus(List.of(new ProductSkuReq(-5L, null, null)));
        assertThrows(IllegalArgumentException.class, () -> validationService.validateForProduct(req, null));
    }

    @Test
    @DisplayName("P-404: Deve rejeitar erpCode duplicado dentro do mesmo payload com 422 DUPLICATE_ERP_CODE_IN_PAYLOAD")
    void shouldRejectDuplicateErpCodeWithinPayload() {
        req.setSkus(List.of(
                new ProductSkuReq(131476L, "Baunilha", null),
                new ProductSkuReq(131476L, "Chocolate", null)
        ));

        UnprocessableEntityException ex = assertThrows(
                UnprocessableEntityException.class,
                () -> validationService.validateForProduct(req, null)
        );
        assertEquals("DUPLICATE_ERP_CODE_IN_PAYLOAD", ex.getCode());
    }

    @Test
    @DisplayName("P-404: Deve rejeitar variação (sabor/apresentação) duplicada normalizada (case/acento) com 422 DUPLICATE_VARIANT_COMBINATION")
    void shouldRejectDuplicateNormalizedVariantCombination() {
        req.setSkus(List.of(
                new ProductSkuReq(131476L, "Limão", "150g"),
                new ProductSkuReq(131477L, "  limao ", "150G")
        ));

        UnprocessableEntityException ex = assertThrows(
                UnprocessableEntityException.class,
                () -> validationService.validateForProduct(req, null)
        );
        assertEquals("DUPLICATE_VARIANT_COMBINATION", ex.getCode());
    }

    @Test
    @DisplayName("P-403: Deve lançar 422 ERP_CODE_NOT_FOUND quando código não existir no catálogo ERP")
    void shouldThrow422WhenErpCodeNotFound() {
        req.setSkus(List.of(new ProductSkuReq(9999999L, null, null)));
        when(erpCatalogCacheService.findByErpCode(9999999L)).thenReturn(Optional.empty());

        UnprocessableEntityException ex = assertThrows(
                UnprocessableEntityException.class,
                () -> validationService.validateForProduct(req, null)
        );
        assertEquals("ERP_CODE_NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("P-403: Deve propagar 503 ERP_CATALOG_UNAVAILABLE quando ERP indisponível")
    void shouldPropagate503WhenErpUnavailable() {
        req.setSkus(List.of(new ProductSkuReq(131476L, null, null)));
        when(erpCatalogCacheService.findByErpCode(131476L))
                .thenThrow(new ServiceUnavailableException("ERP_CATALOG_UNAVAILABLE", "ERP offline"));

        assertThrows(ServiceUnavailableException.class, () -> validationService.validateForProduct(req, null));
    }

    @Test
    @DisplayName("P-406: Deve exigir confirmação explícita quando inativo_pro = 1 e allowInactiveErp = false")
    void shouldRequireExplicitConfirmationForInactiveErpSku() {
        req.setSkus(List.of(new ProductSkuReq(131479L, "Cookies", null)));
        req.setAllowInactiveErp(false);

        when(erpCatalogCacheService.findByErpCode(131479L)).thenReturn(Optional.of(
                ErpCatalogItemSnapshot.builder().erpCode(131479L).description("WHEY COOKIES").inativoPro(true).build()
        ));

        UnprocessableEntityException ex = assertThrows(
                UnprocessableEntityException.class,
                () -> validationService.validateForProduct(req, null)
        );
        assertEquals("ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED", ex.getCode());
        assertNotNull(ex.getDetails());
    }

    @Test
    @DisplayName("P-404: Deve lançar 409 Conflict com nome do produto detentor quando erpCode já vinculado a outro produto")
    void shouldThrow409WhenErpCodeAlreadyMappedToAnotherProduct() {
        req.setSkus(List.of(new ProductSkuReq(131476L, "Baunilha", null)));

        Product existingOwner = new Product();
        existingOwner.setId("other-prod-id");
        existingOwner.setName("100% Pure Whey 900g");

        ProductSku conflictingSku = new ProductSku();
        conflictingSku.setErpCode(131476L);
        conflictingSku.setProduct(existingOwner);

        when(erpCatalogCacheService.findByErpCode(131476L)).thenReturn(Optional.of(
                ErpCatalogItemSnapshot.builder().erpCode(131476L).description("WHEY BAUNILHA").inativoPro(false).build()
        ));
        when(productSkuRepository.findConflictingSkus(anyList(), any()))
                .thenReturn(List.of(conflictingSku));

        ResourceAlreadyExistsException ex = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> validationService.validateForProduct(req, "current-prod-id")
        );
        assertTrue(ex.getMessage().contains("131476"));
        assertTrue(ex.getMessage().contains("100% Pure Whey 900g"));
    }
}
