package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findByName(String name);

    @Query("SELECT p FROM products p WHERE " +
            "cast(function('translate', LOWER(p.name), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') as String) LIKE cast(function('translate', LOWER(CONCAT('%', :searchTerm, '%')), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') as String)")
    Page<Product> findAllFiltered(String searchTerm, Pageable pageable);

    List<Product> findBySupplierId(String supplierId);

    List<Product> findAllByActiveTrue();

    @Query("SELECT DISTINCT p FROM products p LEFT JOIN FETCH p.skus LEFT JOIN FETCH p.category LEFT JOIN FETCH p.supplier")
    List<Product> findAllWithSkus();

    List<Product> findByCurationStatusOrderByCreatedAtDesc(ProductCurationStatus curationStatus);

    @Query("SELECT p FROM products p " +
           "WHERE p.curationStatus = com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus.PENDING_IMAGE " +
           "AND (:onlyFallback = false OR p.category.slug = 'outros' OR p.supplier.slug = 'fitoherb') " +
           "AND (:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR EXISTS (SELECT 1 FROM product_skus s WHERE s.product = p AND CAST(s.erpCode AS string) LIKE CONCAT('%', :search, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> findCurationPending(
            @Param("onlyFallback") boolean onlyFallback,
            @Param("search") String search,
            Pageable pageable
    );
}
