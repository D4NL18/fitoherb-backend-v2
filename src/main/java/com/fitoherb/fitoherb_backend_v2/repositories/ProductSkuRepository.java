package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, String> {

    Optional<ProductSku> findByErpCode(Long erpCode);

    List<ProductSku> findByErpCodeIn(Collection<Long> erpCodes);

    List<ProductSku> findByProductIdOrderBySortOrderAsc(String productId);

    @Query("SELECT s FROM product_skus s JOIN FETCH s.product p WHERE s.erpCode IN :erpCodes AND (:currentProductId IS NULL OR p.id <> :currentProductId)")
    List<ProductSku> findConflictingSkus(
            @Param("erpCodes") Collection<Long> erpCodes,
            @Param("currentProductId") String currentProductId
    );
}
