package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, String> {

    List<Banner> findAllByIsActiveTrueOrderByPositionAsc();

    @Query("SELECT b FROM banners b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Banner> findAllFiltered(@Param("search") String search, Pageable pageable);
}
