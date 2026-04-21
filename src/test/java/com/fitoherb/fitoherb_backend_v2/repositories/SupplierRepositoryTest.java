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
@org.springframework.context.annotation.Import(SupplierRepositoryTest.AuditorConfig.class)
class SupplierRepositoryTest {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private TestEntityManager entityManager;

    @TestConfiguration
    public static class AuditorConfig {
        @Bean(name = "auditorAwareImpl")
        public AuditorAware<String> auditorAwareImpl() {
            return () -> Optional.of("test-user");
        }
    }

    @BeforeEach
    void setup() {
        supplierRepository.deleteAll();

        Supplier s1 = new Supplier();
        s1.setName("Fornecedor de Ervas");
        s1.setSlug("fornecedor-de-ervas");

        Supplier s2 = new Supplier();
        s2.setName("Ervas Naturais");
        s2.setSlug("ervas-naturais");

        Supplier s3 = new Supplier();
        s3.setName("Distribuidora Raízes");
        s3.setSlug("distribuidora-raizes");

        supplierRepository.saveAll(List.of(s1, s2, s3));
    }

    @Nested
    @DisplayName("Filtros de Busca (Query Customizada)")
    class SearchFiltersTests {

        @Test
        void findAllFilteredByPartNameSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Supplier> result = supplierRepository.findAllFiltered("Ervas", pageable);

            assertEquals(2, result.getTotalElements());
            assertTrue(result.getContent().stream().anyMatch(s -> s.getName().contains("Fornecedor")));
            assertTrue(result.getContent().stream().anyMatch(s -> s.getName().contains("Naturais")));
        }

        @Test
        void findAllFilteredCaseInsensitiveSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Supplier> result = supplierRepository.findAllFiltered("DISTRIBUIDORA", pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals("Distribuidora Raízes", result.getContent().get(0).getName());
        }

        @ParameterizedTest
        @CsvSource({
                "'', 3",
                "'Inexistente', 0",
                "'%;--', 0"
        })
        void findAllFilteredEdgeCases(String search, int expectedCount) {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Supplier> result = supplierRepository.findAllFiltered(search, pageable);

            assertEquals(expectedCount, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Consultas de Agrupamento e Contagem")
    class CountQueriesTests {
        @Test
        void countProductsBySupplierSlugSuccess() {
            Supplier supplier = supplierRepository.findBySlug("fornecedor-de-ervas").orElseThrow();

            ProductCategory cat = new ProductCategory();
            cat.setName("Categoria Teste");
            cat.setSlug("categoria-teste");
            entityManager.persist(cat);

            Product p1 = new Product();
            p1.setName("Produto A");
            p1.setSlug("produto-a");
            p1.setCategory(cat);
            p1.setSupplier(supplier);
            entityManager.persist(p1);

            Product p2 = new Product();
            p2.setName("Produto B");
            p2.setSlug("produto-b");
            p2.setCategory(cat);
            p2.setSupplier(supplier);
            entityManager.persist(p2);

            entityManager.flush();

            int count = supplierRepository.countProductsBySupplierSlug("fornecedor-de-ervas");

            assertEquals(2, count);
        }

        @Test
        void countProductsPerSupplierSuccess() {
            Supplier s1 = supplierRepository.findBySlug("fornecedor-de-ervas").orElseThrow();
            Supplier s2 = supplierRepository.findBySlug("ervas-naturais").orElseThrow();

            ProductCategory cat = new ProductCategory();
            cat.setName("Categoria Lote");
            cat.setSlug("categoria-lote");
            entityManager.persist(cat);

            Product p1 = new Product();
            p1.setName("P1");
            p1.setSlug("p1");
            p1.setCategory(cat);
            p1.setSupplier(s1);
            entityManager.persist(p1);

            Product p2 = new Product();
            p2.setName("P2");
            p2.setSlug("p2");
            p2.setCategory(cat);
            p2.setSupplier(s1);
            entityManager.persist(p2);

            Product p3 = new Product();
            p3.setName("P3");
            p3.setSlug("p3");
            p3.setCategory(cat);
            p3.setSupplier(s2);
            entityManager.persist(p3);

            entityManager.flush();

            List<Object[]> results = supplierRepository.countProductsPerSupplier();

            assertFalse(results.isEmpty());

            boolean s1Found = results.stream()
                    .anyMatch(obj -> obj[0].equals("fornecedor-de-ervas") && ((Long) obj[1]) == 2L);

            boolean s2Found = results.stream()
                    .anyMatch(obj -> obj[0].equals("ervas-naturais") && ((Long) obj[1]) == 1L);

            assertTrue(s1Found, "Deveria encontrar 2 produtos para o fornecedor-de-ervas");
            assertTrue(s2Found, "Deveria encontrar 1 produto para as ervas-naturais");
        }
    }
}