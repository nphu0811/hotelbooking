package com.example.demo.repository;

import com.example.demo.entity.Room;
import com.example.demo.entity.RoomStatus;
import com.example.demo.entity.Hotel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    boolean existsByHotel(Hotel hotel);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(RoomStatus status);

    @EntityGraph(attributePaths = {"hotel", "images", "amenities"})
    @Query("""
            select distinct r from Room r
            join r.hotel h
            where r.deleted = false
              and h.deleted = false
              and r.status = :status
              and r.capacity >= :guests
              and (:q = '' or lower(h.city) like lower(concat('%', :q, '%'))
                   or lower(h.province) like lower(concat('%', :q, '%'))
                   or lower(h.name) like lower(concat('%', :q, '%'))
                   or lower(r.name) like lower(concat('%', :q, '%')))
              and (:minPrice is null or r.pricePerNight >= :minPrice)
              and (:maxPrice is null or r.pricePerNight <= :maxPrice)
              and not exists (
                  select b.id from Booking b
                  where b.room = r
                    and b.status in (com.example.demo.entity.BookingStatus.PENDING_PAYMENT,
                                     com.example.demo.entity.BookingStatus.CONFIRMED,
                                     com.example.demo.entity.BookingStatus.CHECKED_IN)
                    and b.checkIn < :checkOut
                    and b.checkOut > :checkIn
              )
            """)
    Page<Room> searchAvailable(
            @Param("q") String q,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("guests") int guests,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") RoomStatus status,
            Pageable pageable);

    @Query("""
            select r.id from Room r
            join r.hotel h
            where r.deleted = false
              and h.deleted = false
              and r.status = :status
              and r.capacity >= :guests
              and (:q = '' or lower(h.city) like lower(concat('%', :q, '%'))
                   or lower(h.province) like lower(concat('%', :q, '%'))
                   or lower(h.name) like lower(concat('%', :q, '%'))
                   or lower(r.name) like lower(concat('%', :q, '%')))
              and (:minPrice is null or r.pricePerNight >= :minPrice)
              and (:maxPrice is null or r.pricePerNight <= :maxPrice)
              and not exists (
                  select b.id from Booking b
                  where b.room = r
                    and b.status in (com.example.demo.entity.BookingStatus.PENDING_PAYMENT,
                                     com.example.demo.entity.BookingStatus.CONFIRMED,
                                     com.example.demo.entity.BookingStatus.CHECKED_IN)
                    and b.checkIn < :checkOut
                    and b.checkOut > :checkIn
              )
            """)
    Page<UUID> searchAvailableIds(
            @Param("q") String q,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("guests") int guests,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") RoomStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"hotel", "images", "amenities"})
    @Query("select distinct r from Room r where r.id in :ids")
    List<Room> findDetailedByIdIn(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = {"hotel", "images", "amenities"})
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> lockById(@Param("id") UUID id);
}
