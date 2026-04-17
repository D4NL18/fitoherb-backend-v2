package com.fitoherb.fitoherb_backend_v2.repositories;

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
@org.springframework.context.annotation.Import(SupplierRepositoryTest.AuditorConfig.class)
class SupplierRepositoryTest {

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
}