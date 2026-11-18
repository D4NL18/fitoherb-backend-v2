package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductVisibilityCalculatorTest {

    private ProductVisibilityCalculator calculator;

    @BeforeEach
    void setup() {
        calculator = new ProductVisibilityCalculator(new ProductVariantSanitizer());
    }

    @Test
    @DisplayName("A3: Produto legado UNMAPPED (0 SKUs) deve preservar active = true")
    void shouldKeepActiveTrueForUnmappedProduct() {
        // Arrange
        Product product = new Product();
        product.setActive(true);
        product.setCurationStatus(ProductCurationStatus.COMPLETE);
        product.setSkus(new ArrayList<>());

        // Act
        calculator.recalculate(product);

        // Assert
        assertTrue(product.getActive());
    }

    @Test
    @DisplayName("P-413: Produto IGNORED deve permanecer active = false mesmo com SKU ativo")
    void shouldForceActiveFalseWhenIgnored() {
        // Arrange
        Product product = new Product();
        product.setActive(true);
        product.setCurationStatus(ProductCurationStatus.IGNORED);

        ProductSku sku = new ProductSku();
        sku.setErpCode(991045L);
        sku.setErpInactive(false);
        product.setSkus(List.of(sku));

        // Act
        calculator.recalculate(product);

        // Assert
        assertFalse(product.getActive());
    }

    @Test
    @DisplayName("P-411 / C-17: 100% dos SKUs inativos deve ocultar produto (active = false) preservando SKUs")
    void shouldDeactivateProductWhenAllSkusAreInactive() {
        // Arrange
        Product product = new Product();
        product.setActive(true);
        product.setCurationStatus(ProductCurationStatus.COMPLETE);
        product.setImagePath("whey.webp");

        ProductSku sku1 = new ProductSku();
        sku1.setErpCode(131476L);
        sku1.setFlavour("Baunilha");
        sku1.setErpInactive(true);
        sku1.setSortOrder(0);

        product.setSkus(List.of(sku1));

        // Act
        calculator.recalculate(product);

        // Assert
        assertFalse(product.getActive());
        assertEquals("whey.webp", product.getImagePath());
        assertEquals(1, product.getSkus().size());
    }

    @Test
    @DisplayName("P-411 / C-18: Inativação parcial Multi-SKU mantém active = true e oculta apenas sabores inativos na projeção")
    void shouldFilterInactiveFlavoursInPartialMultiSku() {
        // Arrange
        Product product = new Product();
        product.setActive(false);
        product.setCurationStatus(ProductCurationStatus.COMPLETE);

        ProductSku baunilha = new ProductSku();
        baunilha.setErpCode(131476L);
        baunilha.setFlavour("Baunilha");
        baunilha.setErpInactive(false);
        baunilha.setSortOrder(0);

        ProductSku chocolate = new ProductSku();
        chocolate.setErpCode(131477L);
        chocolate.setFlavour("Chocolate");
        chocolate.setErpInactive(true);
        chocolate.setSortOrder(1);

        ProductSku morango = new ProductSku();
        morango.setErpCode(131478L);
        morango.setFlavour("Morango");
        morango.setErpInactive(false);
        morango.setSortOrder(2);

        product.setSkus(List.of(baunilha, chocolate, morango));

        // Act
        calculator.recalculate(product);

        // Assert
        assertTrue(product.getActive());
        assertEquals(List.of("Baunilha", "Morango"), product.getFlavours());
        assertEquals(3, product.getSkus().size());
    }

    @Test
    @DisplayName("P-411 & P-412: Reativação de SKU em produto sem foto deve ativar imediatamente mantendo PENDING_IMAGE")
    void shouldReactivateProductWithoutImageImmediately() {
        // Arrange
        Product product = new Product();
        product.setActive(false);
        product.setImagePath(null);
        product.setCurationStatus(ProductCurationStatus.PENDING_IMAGE);

        ProductSku sku = new ProductSku();
        sku.setErpCode(991001L);
        sku.setErpInactive(false);
        sku.setSortOrder(0);
        product.setSkus(List.of(sku));

        // Act
        calculator.recalculate(product);

        // Assert
        assertTrue(product.getActive());
        assertEquals(ProductCurationStatus.PENDING_IMAGE, product.getCurationStatus());
    }
}
