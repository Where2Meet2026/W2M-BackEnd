package com.w2m.backend.rag.repository;

import com.w2m.backend.rag.entity.PlaceDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceDescriptionRepository extends JpaRepository<PlaceDescription, Long> {

    Optional<PlaceDescription> findByPlaceId(String placeId);

    List<PlaceDescription> findByPlaceIdIn(Collection<String> placeIds);
}
