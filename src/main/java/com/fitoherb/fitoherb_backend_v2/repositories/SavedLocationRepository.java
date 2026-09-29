package com.fitoherb.fitoherb_backend_v2.repositories;

import com.fitoherb.fitoherb_backend_v2.entities.SavedLocation;
import com.fitoherb.fitoherb_backend_v2.enums.SavedLocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedLocationRepository extends JpaRepository<SavedLocation, String> {

    List<SavedLocation> findAllByUserEmailOrderByCreatedAtDesc(String email);

    List<SavedLocation> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<SavedLocation> findAllByUserEmailAndTypeOrderByCreatedAtDesc(String email, SavedLocationType type);

    Optional<SavedLocation> findFirstByUserEmailAndType(String email, SavedLocationType type);

    Optional<SavedLocation> findFirstByUserIdAndType(String userId, SavedLocationType type);

    @Modifying
    @Query("UPDATE saved_locations s SET s.type = :favoriteType WHERE s.user.id = :userId AND s.type = :baseType")
    void unsetPreviousBaseLocations(
            @Param("userId") String userId,
            @Param("favoriteType") SavedLocationType favoriteType,
            @Param("baseType") SavedLocationType baseType
    );
}
