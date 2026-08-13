package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, String> {

    List<Banner> findAllByIsActiveTrueOrderByPositionAsc();

    @Query("SELECT b FROM banners b WHERE cast(function('translate', LOWER(b.title), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') as String) LIKE cast(function('translate', LOWER(CONCAT('%', :search, '%')), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') as String)")
    Page<Banner> findAllFiltered(@Param("search") String search, Pageable pageable);

    @Modifying
    @Query("UPDATE banners b SET b.position = b.position + 1 WHERE b.position >= :newPosition")
    void shiftPositionsUp(@Param("newPosition") int newPosition);

    @Modifying
    @Query("UPDATE banners b SET b.position = b.position + 1 WHERE b.position >= :newPosition AND b.position < :oldPosition")
    void shiftPositionsUpForUpdate(@Param("newPosition") int newPosition, @Param("oldPosition") int oldPosition);

    @Modifying
    @Query("UPDATE banners b SET b.position = b.position - 1 WHERE b.position <= :newPosition AND b.position > :oldPosition")
    void shiftPositionsDownForUpdate(@Param("newPosition") int newPosition, @Param("oldPosition") int oldPosition);

    @Modifying
    @Query("UPDATE banners b SET b.position = b.position - 1 WHERE b.position > :deletedPosition")
    void shiftPositionsDown(@Param("deletedPosition") int deletedPosition);
}
