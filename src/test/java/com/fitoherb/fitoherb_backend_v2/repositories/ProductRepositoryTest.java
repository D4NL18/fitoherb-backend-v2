package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import(ProductRepositoryTest.AuditorConfig.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @TestConfiguration
    public static class AuditorConfig {
        @Bean(name = "auditorAwareImpl")
        public AuditorAware<String> auditorAwareImpl() {
            return () -> Optional.of("test-user");
        }
    }

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        supplierRepository.deleteAll();

        ProductCategory category = new ProductCategory();
        category.setName("Categoria Teste");
        category.setSlug("categoria-teste");
        categoryRepository.save(category);

        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor Teste");
        supplier.setSlug("fornecedor-teste");
        supplierRepository.save(supplier);

        Product p1 = new Product();
        p1.setName("Chá Verde");
        p1.setSlug("cha-verde");
        p1.setCategory(category);
        p1.setSupplier(supplier);

        Product p2 = new Product();
        p2.setName("Cápsula de Erva Mate");
        p2.setSlug("capsula-de-erva-mate");
        p2.setCategory(category);
        p2.setSupplier(supplier);

        Product p3 = new Product();
        p3.setName("Erva Doce");
        p3.setSlug("erva-doce");
        p3.setCategory(category);
        p3.setSupplier(supplier);

        productRepository.saveAll(List.of(p1, p2, p3));
    }

    @Nested
    @DisplayName("Filtros de Busca (Query Customizada)")
    class SearchFiltersTests {

        @Test
        void findAllFilteredByPartNameSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> result = productRepository.findAllFiltered("Erva", pageable);

            assertEquals(2, result.getTotalElements());
            assertTrue(result.getContent().stream().anyMatch(p -> p.getName().contains("Cápsula")));
            assertTrue(result.getContent().stream().anyMatch(p -> p.getName().contains("Doce")));
        }

        @Test
        void findAllFilteredCaseInsensitiveSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> result = productRepository.findAllFiltered("CHÁ", pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals("Chá Verde", result.getContent().get(0).getName());
        }

        @ParameterizedTest
        @CsvSource({
                "'', 3",
                "'Inexistente', 0",
                "'%;--', 0"
        })
        void findAllFilteredEdgeCases(String search, int expectedCount) {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> result = productRepository.findAllFiltered(search, pageable);

            assertEquals(expectedCount, result.getTotalElements());
        }
    }
}