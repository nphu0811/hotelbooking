package com.example.demo.repository;

import com.example.demo.entity.Booking;
import com.example.demo.entity.Review;
import com.example.demo.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByBooking(Booking booking);

    List<Review> findTop5ByRoomOrderByCreatedAtDesc(Room room);

    List<Review> findByRoom(Room room);
}
