package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.enums.UserRole;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import(UserRepositoryTest.AuditorConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @TestConfiguration
    public static class AuditorConfig {
        @Bean(name = "auditorAwareImpl")
        public AuditorAware<String> auditorAwareImpl() {
            return () -> Optional.of("test-user");
        }
    }

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        User u1 = new User();
        u1.setName("Daniel Marinho");
        u1.setEmail("daniel@fitoherb.com");
        u1.setPassword("pass123");
        u1.setBirthDate(LocalDate.of(1990, 1, 1));
        u1.setRole(UserRole.ADMIN);

        User u2 = new User();
        u2.setName("Maria Silva");
        u2.setEmail("maria.silva@test.com");
        u2.setPassword("pass123");
        u2.setBirthDate(LocalDate.of(1995, 5, 5));
        u2.setRole(UserRole.USER);

        User u3 = new User();
        u3.setName("João Sousa");
        u3.setEmail("joao.sousa@fitoherb.com");
        u3.setPassword("pass123");
        u3.setBirthDate(LocalDate.of(1985, 10, 10));
        u3.setRole(UserRole.ADMIN);

        userRepository.saveAll(List.of(u1, u2, u3));
    }

    @Nested
    @DisplayName("Filtros de Busca (Query Customizada)")
    class SearchFiltersTests {

        @Test
        void findAllFilteredByNameSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> result = userRepository.findAllFiltered("Silva", pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals("Maria Silva", result.getContent().get(0).getName());
        }

        @Test
        void findAllFilteredByEmailSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> result = userRepository.findAllFiltered("@fitoherb.com", pageable);

            assertEquals(2, result.getTotalElements());
            assertTrue(result.getContent().stream().anyMatch(u -> u.getName().equals("Daniel Marinho")));
            assertTrue(result.getContent().stream().anyMatch(u -> u.getName().equals("João Sousa")));
        }

        @Test
        void findAllFilteredCaseInsensitiveSuccess() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> result = userRepository.findAllFiltered("DANIEL", pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals("daniel@fitoherb.com", result.getContent().get(0).getEmail());
        }

        @ParameterizedTest
        @CsvSource({
                "'', 3",
                "'Inexistente', 0",
                "'%;--', 0"
        })
        void findAllFilteredEdgeCases(String search, int expectedCount) {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> result = userRepository.findAllFiltered(search, pageable);

            assertEquals(expectedCount, result.getTotalElements());
        }
    }
}