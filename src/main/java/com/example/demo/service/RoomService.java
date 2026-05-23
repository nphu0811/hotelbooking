package com.example.demo.service;

import com.example.demo.entity.Room;
import com.example.demo.entity.RoomStatus;
import com.example.demo.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Page<Room> search(String query,
                             LocalDate checkIn,
                             LocalDate checkOut,
                             int guests,
                             BigDecimal minPrice,
                             BigDecimal maxPrice,
                             String sort,
                             int page) {
        validateSearch(checkIn, checkOut, guests);
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, sortFor(sort));
        return roomRepository.searchAvailable(
                query == null ? "" : query.trim(),
                checkIn,
                checkOut,
                guests,
                minPrice,
                maxPrice,
                RoomStatus.AVAILABLE,
                pageable);
    }

    public Room requireDetail(UUID id) {
        Room room = roomRepository.findDetailedById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng"));
        if (room.isDeleted()) {
            throw new BusinessException("Không tìm thấy phòng");
        }
        return room;
    }

    private void validateSearch(LocalDate checkIn, LocalDate checkOut, int guests) {
        LocalDate today = LocalDate.now();
        if (checkIn == null || checkIn.isBefore(today)) {
            throw new BusinessException("Ngày nhận phòng không hợp lệ, vui lòng chọn từ hôm nay trở đi");
        }
        if (checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessException("Ngày trả phòng phải sau ngày nhận phòng");
        }
        if (guests < 1 || guests > 10) {
            throw new BusinessException("Số lượng khách phải từ 1 đến 10 người");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > 30) {
            throw new BusinessException("Thời gian lưu trú tối đa 30 đêm");
        }
    }

    private Sort sortFor(String sort) {
        if ("price_asc".equals(sort)) {
            return Sort.by("pricePerNight").ascending();
        }
        if ("price_desc".equals(sort)) {
            return Sort.by("pricePerNight").descending();
        }
        if ("newest".equals(sort)) {
            return Sort.by("createdAt").descending();
        }
        return Sort.by("averageRating").descending().and(Sort.by("reviewCount").descending());
    }
}
