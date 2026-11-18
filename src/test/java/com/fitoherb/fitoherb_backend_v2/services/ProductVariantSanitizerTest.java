package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductVariantSanitizerTest {

    private ProductVariantSanitizer sanitizer;

    @BeforeEach
    void setup() {
        sanitizer = new ProductVariantSanitizer();
    }

    @Nested
    @DisplayName("P-401: Higienização Atômica e Desmembramento")
    class SanitizationTests {

        @Test
        @DisplayName("Deve desmembrar conectivos ' e ', '/' e vírgulas removendo ponto final residual")
        void shouldSplitLegacyConcatenationsAndStripTrailingDots() {
            // Arrange
            List<String> rawFlavours = List.of("Baunilha", "Chocolate e Morango.");
            List<String> rawPresentations = List.of("25ml e 60ml.", "150g / 300g");

            // Act
            List<String> cleanFlavours = sanitizer.sanitizeList(rawFlavours);
            List<String> cleanPresentations = sanitizer.sanitizeList(rawPresentations);

            // Assert
            assertEquals(List.of("Baunilha", "Chocolate", "Morango"), cleanFlavours);
            assertEquals(List.of("25ml", "60ml", "150g", "300g"), cleanPresentations);
        }

        @Test
        @DisplayName("Deve remover duplicatas preservando a ordem e normalizar espaços")
        void shouldDeduplicateAndCollapseWhitespace() {
            // Arrange
            List<String> raw = List.of("  Fruit   Punch ", "Limão.", "fruit punch", "Melancia");

            // Act
            List<String> result = sanitizer.sanitizeList(raw);

            // Assert
            assertEquals(List.of("Fruit Punch", "Limão", "Melancia"), result);
        }

        @Test
        @DisplayName("Deve rejeitar variação explícita composta apenas por pontuação ou espaços")
        void shouldRejectBlankOrPunctuationOnlyVariant() {
            // Arrange & Act & Assert
            assertThrows(IllegalArgumentException.class, () -> sanitizer.sanitizeSingle("   "));
            assertThrows(IllegalArgumentException.class, () -> sanitizer.sanitizeSingle("."));
            assertThrows(IllegalArgumentException.class, () -> sanitizer.sanitizeSingle("..."));
        }

        @Test
        @DisplayName("Deve retornar null quando variação individual opcional for null")
        void shouldReturnNullWhenSingleVariantIsNull() {
            // Act
            String result = sanitizer.sanitizeSingleOrNull(null);

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("P-401 & P-411: Projeção de Sabores e Apresentações a partir de SKUs")
    class ProjectionTests {

        @Test
        @DisplayName("Deve projetar flavours e presentation deduplicados ignorando SKUs com erpInactive = true")
        void shouldProjectActiveDistinctVariantsInOrder() {
            // Arrange
            ProductSku s1 = new ProductSku();
            s1.setFlavour("Baunilha");
            s1.setPresentation("150g");
            s1.setSortOrder(0);
            s1.setErpInactive(false);

            ProductSku s2 = new ProductSku();
            s2.setFlavour("Chocolate");
            s2.setPresentation("150g");
            s2.setSortOrder(1);
            s2.setErpInactive(true);

            ProductSku s3 = new ProductSku();
            s3.setFlavour("Morango");
            s3.setPresentation("300g");
            s3.setSortOrder(2);
            s3.setErpInactive(false);

            ProductSku s4 = new ProductSku();
            s4.setFlavour("Baunilha");
            s4.setPresentation("300g");
            s4.setSortOrder(3);
            s4.setErpInactive(false);

            List<ProductSku> skus = List.of(s1, s2, s3, s4);

            // Act
            List<String> flavours = sanitizer.projectActiveFlavours(skus);
            List<String> presentations = sanitizer.projectActivePresentations(skus);

            // Assert
            assertEquals(List.of("Baunilha", "Morango"), flavours);
            assertEquals(List.of("150g", "300g"), presentations);
        }

        @Test
        @DisplayName("Deve retornar listas vazias quando todos os SKUs estiverem inativos no ERP")
        void shouldReturnEmptyListsWhenAllSkusInactive() {
            // Arrange
            ProductSku s1 = new ProductSku();
            s1.setFlavour("Limão");
            s1.setPresentation("150g");
            s1.setErpInactive(true);

            // Act
            List<String> flavours = sanitizer.projectActiveFlavours(List.of(s1));
            List<String> presentations = sanitizer.projectActivePresentations(List.of(s1));

            // Assert
            assertTrue(flavours.isEmpty());
            assertTrue(presentations.isEmpty());
        }
    }
}
