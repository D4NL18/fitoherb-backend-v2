package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);

    Optional<Product> findByName(String name);

    @Query("SELECT p FROM products p WHERE " +
            "function('unaccent', LOWER(p.name)) LIKE function('unaccent', LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Product> findAllFiltered(String searchTerm, Pageable pageable);

    List<Product> findBySupplierId(String supplierId);
}
