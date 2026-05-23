package com.example.demo.repository;

import com.example.demo.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    Optional<Amenity> findByName(String name);
}
