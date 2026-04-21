package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, String> {

    Optional<Supplier> findBySlug(String slug);

    Optional<Supplier> findByName(String name);

    @Query("SELECT s FROM suppliers s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Supplier> findAllFiltered(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(p) FROM products p WHERE p.supplier.slug = :supplierSlug")
    int countProductsBySupplierSlug(@Param("supplierSlug") String supplierSlug);

    @Query("SELECT p.supplier.slug, COUNT(p) FROM products p GROUP BY p.supplier.slug")
    List<Object[]> countProductsPerSupplier();
}
