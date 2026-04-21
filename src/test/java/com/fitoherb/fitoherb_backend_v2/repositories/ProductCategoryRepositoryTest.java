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
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
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
@org.springframework.context.annotation.Import(ProductCategoryRepositoryTest.AuditorConfig.class)
class ProductCategoryRepositoryTest {

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @TestConfiguration
    static class AuditorConfig {
        @Bean(name = "auditorAwareImpl")
        public AuditorAware<String> auditorAwareImpl() {
            return () -> Optional.of("test-user");
        }
    }

    @BeforeEach
    void setup() {
        categoryRepository.deleteAll();

        ProductCategory cat1 = new ProductCategory();
        cat1.setName("Chás Naturais");
        cat1.setSlug("chas-naturais");

        ProductCategory cat2 = new ProductCategory();
        cat2.setName("Cápsulas de Ervas");
        cat2.setSlug("capsulas-de-ervas");

        ProductCategory cat3 = new ProductCategory();
        cat3.setName("Ervas Secas");
        cat3.setSlug("ervas-secas");

        categoryRepository.save(cat1);
        categoryRepository.save(cat2);
        categoryRepository.save(cat3);
    }

    @Nested
    @DisplayName("Filtros de Busca (Query Customizada)")
    class SearchFiltersTests {

        @Test
        void findAllFilteredByPartNameSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProductCategory> result = categoryRepository.findAllFiltered("Ervas", pageable);

            assertEquals(2, result.getTotalElements());
            assertTrue(result.getContent().stream().anyMatch(c -> c.getName().contains("Cápsulas")));
            assertTrue(result.getContent().stream().anyMatch(c -> c.getName().contains("Secas")));
        }

        @Test
        void findAllFilteredCaseInsensitiveSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProductCategory> result = categoryRepository.findAllFiltered("CHÁS", pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals("Chás Naturais", result.getContent().get(0).getName());
        }

        @ParameterizedTest
        @CsvSource({
                "'', 3",
                "'Suplementos', 0",
                "'%;--', 0"
        })
        void findAllFilteredEdgeCases(String search, int expectedCount) {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProductCategory> result = categoryRepository.findAllFiltered(search, pageable);

            assertEquals(expectedCount, result.getTotalElements());
        }
    }

    @Test
    void countProductsByCategorySlugSuccess() {
        ProductCategory cat = categoryRepository.findBySlug("chas-naturais").orElseThrow();

        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor Teste");
        supplier.setSlug("fornecedor-teste");
        entityManager.persist(supplier);

        Product p1 = new Product();
        p1.setName("Camomila");
        p1.setSlug("camomila");
        p1.setCategory(cat);
        p1.setSupplier(supplier);
        entityManager.persist(p1);

        Product p2 = new Product();
        p2.setName("Hortelã");
        p2.setSlug("hortela");
        p2.setCategory(cat);
        p2.setSupplier(supplier);
        entityManager.persist(p2);

        entityManager.flush();

        int count = categoryRepository.countProductsByCategorySlug("chas-naturais");

        assertEquals(2, count);
    }

    @Test
    void countProductsPerCategorySuccess() {
        ProductCategory cat1 = categoryRepository.findBySlug("chas-naturais").orElseThrow();
        ProductCategory cat2 = categoryRepository.findBySlug("capsulas-de-ervas").orElseThrow();

        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor Lote");
        supplier.setSlug("fornecedor-lote");
        entityManager.persist(supplier);

        Product p1 = new Product();
        p1.setName("P1");
        p1.setSlug("p1");
        p1.setCategory(cat1);
        p1.setSupplier(supplier);
        entityManager.persist(p1);

        Product p2 = new Product();
        p2.setName("P2");
        p2.setSlug("p2");
        p2.setCategory(cat1);
        p2.setSupplier(supplier);
        entityManager.persist(p2);

        Product p3 = new Product();
        p3.setName("P3");
        p3.setSlug("p3");
        p3.setCategory(cat2);
        p3.setSupplier(supplier);
        entityManager.persist(p3);

        entityManager.flush();

        List<Object[]> results = categoryRepository.countProductsPerCategory();

        assertFalse(results.isEmpty());

        boolean cat1Found = results.stream()
                .anyMatch(obj -> obj[0].equals("chas-naturais") && ((Long) obj[1]) == 2L);

        boolean cat2Found = results.stream()
                .anyMatch(obj -> obj[0].equals("capsulas-de-ervas") && ((Long) obj[1]) == 1L);

        assertTrue(cat1Found, "Deveria encontrar 2 produtos para a categoria chas-naturais");
        assertTrue(cat2Found, "Deveria encontrar 1 produto para a categoria capsulas-de-ervas");
    }
}