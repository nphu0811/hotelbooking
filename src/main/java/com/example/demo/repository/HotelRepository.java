package com.example.demo.repository;

import com.example.demo.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {
    Optional<Hotel> findBySourceAndSourceExternalId(String source, String sourceExternalId);

    long countByDeletedFalse();

    long countBySourceAndDeletedFalse(String source);

    Optional<Hotel> findByIdAndDeletedFalse(UUID id);

    Page<Hotel> findAllByDeletedFalse(Pageable pageable);

    @Query("""
            select h from Hotel h
            where h.deleted = false
              and (:keyword = '' or lower(h.name) like lower(concat('%', :keyword, '%'))
                   or lower(h.city) like lower(concat('%', :keyword, '%'))
                   or lower(h.province) like lower(concat('%', :keyword, '%'))
                   or lower(h.address) like lower(concat('%', :keyword, '%'))
                   or (:keywordAscii <> '' and (
                       lower(h.name) like lower(concat('%', :keywordAscii, '%'))
                       or lower(h.city) like lower(concat('%', :keywordAscii, '%'))
                       or lower(h.province) like lower(concat('%', :keywordAscii, '%'))
                       or lower(h.address) like lower(concat('%', :keywordAscii, '%'))
                   )))
              and (:city = '' or lower(h.city) like lower(concat('%', :city, '%'))
                   or (:cityAscii <> '' and lower(h.city) like lower(concat('%', :cityAscii, '%'))))
              and (:minRating is null or coalesce(h.starRating, 0) >= :minRating)
            """)
    Page<Hotel> searchActive(@Param("keyword") String keyword,
                             @Param("keywordAscii") String keywordAscii,
                             @Param("city") String city,
                             @Param("cityAscii") String cityAscii,
                             @Param("minRating") Integer minRating,
                             Pageable pageable);

    @Query("""
            select h from Hotel h
            where lower(h.name) = lower(:name)
              and h.latitude is not null
              and h.longitude is not null
              and h.latitude between :minLat and :maxLat
              and h.longitude between :minLng and :maxLng
            """)
    Optional<Hotel> findNearbyByName(@Param("name") String name,
                                     @Param("minLat") BigDecimal minLat,
                                     @Param("maxLat") BigDecimal maxLat,
                                     @Param("minLng") BigDecimal minLng,
                                     @Param("maxLng") BigDecimal maxLng);

    @Query("""
            select h from Hotel h
            where lower(h.name) = lower(:name)
              and upper(h.source) = upper(:source)
              and h.latitude is not null
              and h.longitude is not null
              and h.latitude between :minLat and :maxLat
              and h.longitude between :minLng and :maxLng
            """)
    Optional<Hotel> findNearbyByNameAndSource(@Param("name") String name,
                                             @Param("source") String source,
                                             @Param("minLat") BigDecimal minLat,
                                             @Param("maxLat") BigDecimal maxLat,
                                             @Param("minLng") BigDecimal minLng,
                                             @Param("maxLng") BigDecimal maxLng);
}
