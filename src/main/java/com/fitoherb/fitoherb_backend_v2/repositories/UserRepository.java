package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM users u WHERE " +
            "function('unaccent', LOWER(u.name)) LIKE function('unaccent', LOWER(CONCAT('%', :search, '%'))) OR " +
            "function('unaccent', LOWER(u.email)) LIKE function('unaccent', LOWER(CONCAT('%', :search, '%')))")
    Page<User> findAllFiltered(@Param("search") String search, Pageable pageable);
}
