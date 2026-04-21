package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {
    Optional<ProductCategory> findBySlug(String slug);

    Optional<ProductCategory> findByName(String name);

    @Query("SELECT pc FROM product_categories pc WHERE " +
            "LOWER(pc.name) LIKE LOWER(CONCAT('%', :search, '%'))"
    )
    Page<ProductCategory> findAllFiltered(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(p) FROM products p WHERE p.category.slug = :categorySlug")
    int countProductsByCategorySlug(@Param("categorySlug") String categorySlug);

    @Query("SELECT p.category.slug, COUNT(p) FROM products p GROUP BY p.category.slug")
    List<Object[]> countProductsPerCategory();
}
