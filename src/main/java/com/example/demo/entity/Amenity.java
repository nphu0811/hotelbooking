package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "amenities")
public class Amenity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "amenity_id")
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    private String icon;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Amenity() {
    }

    public Amenity(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}
