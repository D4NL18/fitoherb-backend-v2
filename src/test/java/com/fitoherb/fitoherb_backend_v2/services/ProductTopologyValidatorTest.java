package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.ProductSkuReq;
import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import com.fitoherb.fitoherb_backend_v2.exceptions.UnprocessableEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductTopologyValidatorTest {

    private ProductTopologyValidator validator;

    @BeforeEach
    void setup() {
        validator = new ProductTopologyValidator();
    }

    @Nested
    @DisplayName("P-405: Topologias Válidas")
    class ValidTopologiesTests {

        @Test
        @DisplayName("Deve validar produto SIMPLE com exatamente 1 SKU sem sabor nem apresentação")
        void shouldValidateSimpleTopology() {
            // Arrange
            List<ProductSkuReq> skus = List.of(new ProductSkuReq(440100L, null, null));

            // Act
            ProductTopology topology = validator.validateAndInfer(skus);

            // Assert
            assertEquals(ProductTopology.SIMPLE, topology);
        }

        @Test
        @DisplayName("Deve validar produto FLAVOUR_ONLY com múltiplos sabores e apresentação nula")
        void shouldValidateFlavourOnlyTopology() {
            // Arrange
            List<ProductSkuReq> skus = List.of(
                    new ProductSkuReq(131476L, "Baunilha", null),
                    new ProductSkuReq(131477L, "Chocolate", null),
                    new ProductSkuReq(131478L, "Morango", null)
            );

            // Act
            ProductTopology topology = validator.validateAndInfer(skus);

            // Assert
            assertEquals(ProductTopology.FLAVOUR_ONLY, topology);
        }

        @Test
        @DisplayName("Deve validar produto PRESENTATION_ONLY com múltiplas apresentações e sabor nulo")
        void shouldValidatePresentationOnlyTopology() {
            // Arrange
            List<ProductSkuReq> skus = List.of(
                    new ProductSkuReq(580000L, null, "25ml"),
                    new ProductSkuReq(580001L, null, "60ml")
            );

            // Act
            ProductTopology topology = validator.validateAndInfer(skus);

            // Assert
            assertEquals(ProductTopology.PRESENTATION_ONLY, topology);
        }

        @Test
        @DisplayName("Deve validar produto MATRIX esparsa sem exigir combinações ausentes")
        void shouldValidateSparseMatrixTopology() {
            // Arrange
            List<ProductSkuReq> skus = List.of(
                    new ProductSkuReq(90101L, "Fruit Punch", "150g"),
                    new ProductSkuReq(90102L, "Limão", "150g"),
                    new ProductSkuReq(90103L, "Melancia", "150g"),
                    new ProductSkuReq(90201L, "Fruit Punch", "300g"),
                    new ProductSkuReq(90202L, "Limão", "300g")
            );

            // Act
            ProductTopology topology = validator.validateAndInfer(skus);

            // Assert
            assertEquals(ProductTopology.MATRIX, topology);
        }
    }

    @Nested
    @DisplayName("P-405: Rejeição de Estruturas Inválidas e Hibridismo")
    class InvalidTopologiesTests {

        @Test
        @DisplayName("Deve rejeitar lista de SKUs nula ou vazia com 422 PRODUCT_TOPOLOGY_INVALID")
        void shouldRejectNullOrEmptySkus() {
            UnprocessableEntityException exNull = assertThrows(
                    UnprocessableEntityException.class,
                    () -> validator.validateAndInfer(null)
            );
            assertEquals("PRODUCT_TOPOLOGY_INVALID", exNull.getCode());

            UnprocessableEntityException exEmpty = assertThrows(
                    UnprocessableEntityException.class,
                    () -> validator.validateAndInfer(Collections.emptyList())
            );
            assertEquals("PRODUCT_TOPOLOGY_INVALID", exEmpty.getCode());
        }

        @Test
        @DisplayName("Deve rejeitar múltiplos SKUs sem sabor nem apresentação")
        void shouldRejectMultipleSimpleSkus() {
            // Arrange
            List<ProductSkuReq> skus = List.of(
                    new ProductSkuReq(1001L, null, null),
                    new ProductSkuReq(1002L, null, null)
            );

            // Act & Assert
            UnprocessableEntityException ex = assertThrows(
                    UnprocessableEntityException.class,
                    () -> validator.validateAndInfer(skus)
            );
            assertEquals("PRODUCT_TOPOLOGY_INVALID", ex.getCode());
        }

        @Test
        @DisplayName("Deve rejeitar mistura de topologias (ex.: simples + sabor ou sabor + matriz)")
        void shouldRejectMixedTopologies() {
            // Arrange
            List<ProductSkuReq> simplePlusFlavour = List.of(
                    new ProductSkuReq(1001L, null, null),
                    new ProductSkuReq(1002L, "Chocolate", null)
            );
            List<ProductSkuReq> flavourPlusMatrix = List.of(
                    new ProductSkuReq(2001L, "Baunilha", null),
                    new ProductSkuReq(2002L, "Chocolate", "900g")
            );

            // Act & Assert
            UnprocessableEntityException ex1 = assertThrows(
                    UnprocessableEntityException.class,
                    () -> validator.validateAndInfer(simplePlusFlavour)
            );
            assertEquals("PRODUCT_TOPOLOGY_INVALID", ex1.getCode());

            UnprocessableEntityException ex2 = assertThrows(
                    UnprocessableEntityException.class,
                    () -> validator.validateAndInfer(flavourPlusMatrix)
            );
            assertEquals("PRODUCT_TOPOLOGY_INVALID", ex2.getCode());
        }

        @Test
        @DisplayName("Deve rejeitar produto excedendo MAX_SKUS_PER_PRODUCT (100)")
        void shouldRejectMoreThanMaxSkus() {
            // Arrange
            List<ProductSkuReq> skus = new ArrayList<>();
            for (int i = 1; i <= 101; i++) {
                skus.add(new ProductSkuReq((long) i, "Sabor " + i, null));
            }

            // Act & Assert
            assertThrows(UnprocessableEntityException.class, () -> validator.validateAndInfer(skus));
        }
    }
}
