package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ApiEnvelope;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.IgnoreCurationProductRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.QuickCurationPendingItemRes;
import com.fitoherb.fitoherb_backend_v2.entities.*;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.infra.clients.AiCatalogRefreshClient;
import com.fitoherb.fitoherb_backend_v2.repositories.ErpIgnoredCatalogCodeRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpCurationServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ErpIgnoredCatalogCodeRepository ignoredCatalogCodeRepository;
    @Mock private AiCatalogRefreshClient aiCatalogRefreshClient;

    @InjectMocks
    private ErpCurationService curationService;

    @Test
    @DisplayName("P-413: Deve listar produtos PENDING_IMAGE sinalizando fallback de categoria/fornecedor")
    void shouldListPendingImageProductsWithFallbackWarnings() {
        // Arrange
        ProductCategory fallbackCat = new ProductCategory();
        fallbackCat.setSlug("outros");
        fallbackCat.setName("Outros");

        Supplier sup = new Supplier();
        sup.setSlug("black-skull");
        sup.setName("Black Skull");

        Product pending = new Product();
        pending.setSlug("creatina-turbo-black-skull");
        pending.setName("Creatina Turbo");
        pending.setActive(true);
        pending.setCurationStatus(ProductCurationStatus.PENDING_IMAGE);
        pending.setImagePath(null);
        pending.setCategory(fallbackCat);
        pending.setSupplier(sup);
        pending.setCreatedAt(LocalDateTime.now());
        pending.setCreatedBy("operador@fitoherb.com");

        ProductSku sku = new ProductSku();
        sku.setId("sku-1");
        sku.setErpCode(991050L);
        sku.setErpInactive(false);
        sku.setErpInactiveConfirmed(false);
        pending.setSkus(List.of(sku));

        Page<Product> page = new PageImpl<>(List.of(pending));
        when(productRepository.findCurationPending(anyBoolean(), any(), any(Pageable.class))).thenReturn(page);

        // Act
        ApiEnvelope<List<QuickCurationPendingItemRes>> result = curationService.getCurationPending(0, 20, false, null);

        // Assert
        assertEquals(1, result.getData().size());
        QuickCurationPendingItemRes item = result.getData().get(0);
        assertEquals("creatina-turbo-black-skull", item.getSlug());
        assertEquals(ProductCurationStatus.PENDING_IMAGE, item.getCurationStatus());
        assertNull(item.getImageUrl());
        assertTrue(item.isFallbackCategoryWarning());
        assertFalse(item.isFallbackSupplierWarning());
        assertEquals(1, item.getSkus().size());
        assertEquals(991050L, item.getSkus().get(0).getErpCode());
    }

    @Test
    @DisplayName("P-413 / C-22: Ignorar pendência por slug deve ocultar produto, gravar IGNORED e bloquear erpCode em erp_ignored_catalog_codes")
    void shouldIgnorePendingProductBySlugAndPersistIgnoredCodes() {
        // Arrange
        Product pending = new Product();
        pending.setId("prod-ignore");
        pending.setSlug("squeeze-evento-fitoherb");
        pending.setName("Squeeze Evento");
        pending.setActive(true);
        pending.setCurationStatus(ProductCurationStatus.PENDING_IMAGE);

        ProductSku sku = new ProductSku();
        sku.setErpCode(990999L);
        pending.setSkus(List.of(sku));

        when(productRepository.findBySlug("squeeze-evento-fitoherb")).thenReturn(Optional.of(pending));
        when(aiCatalogRefreshClient.refreshCatalog()).thenReturn(true);

        // Act
        IgnoreCurationProductRes res = curationService.ignorePendingProduct("squeeze-evento-fitoherb", "Brinde interno", "admin@fitoherb.com");

        // Assert
        assertFalse(pending.getActive());
        assertEquals(ProductCurationStatus.IGNORED, pending.getCurationStatus());
        assertFalse(res.isActive());
        assertEquals(ProductCurationStatus.IGNORED, res.getCurationStatus());
        assertEquals(List.of(990999L), res.getIgnoredErpCodes());

        ArgumentCaptor<ErpIgnoredCatalogCode> ignoredCaptor = ArgumentCaptor.forClass(ErpIgnoredCatalogCode.class);
        verify(ignoredCatalogCodeRepository).save(ignoredCaptor.capture());
        assertEquals(990999L, ignoredCaptor.getValue().getErpCode());
        assertEquals("ADMIN_IGNORED", ignoredCaptor.getValue().getSource());
        verify(aiCatalogRefreshClient).refreshCatalog();
    }

    @Test
    @DisplayName("P-413: Deve lançar ResourceNotFoundException (404) ao tentar ignorar slug inexistente")
    void shouldThrow404WhenIgnoringNonExistentSlug() {
        // Arrange
        when(productRepository.findBySlug("slug-inexistente")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> curationService.ignorePendingProduct("slug-inexistente", "motivo", "admin@fitoherb.com")
        );
        verifyNoInteractions(ignoredCatalogCodeRepository);
    }
}
