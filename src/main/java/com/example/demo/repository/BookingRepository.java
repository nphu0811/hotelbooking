package com.example.demo.repository;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query("""
            select count(b) from Booking b
            where b.room = :room
              and b.status in :statuses
              and b.checkIn < :checkOut
              and b.checkOut > :checkIn
            """)
    long countOverlaps(@Param("room") Room room,
                       @Param("checkIn") LocalDate checkIn,
                       @Param("checkOut") LocalDate checkOut,
                       @Param("statuses") Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = {"room", "room.hotel"})
    Page<Booking> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "room.hotel", "user"})
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findDetailedById(@Param("id") UUID id);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant expiresAt);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
}
