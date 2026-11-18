package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.ErpIgnoredCatalogCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface ErpIgnoredCatalogCodeRepository extends JpaRepository<ErpIgnoredCatalogCode, Long> {

    @Query("SELECT c.erpCode FROM erp_ignored_catalog_codes c")
    Set<Long> findAllErpCodes();
}
