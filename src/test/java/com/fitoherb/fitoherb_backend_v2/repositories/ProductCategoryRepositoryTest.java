package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import(ProductCategoryRepositoryTest.AuditorConfig.class)
class ProductCategoryRepositoryTest {

    @Autowired
    private ProductCategoryRepository categoryRepository;

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
}