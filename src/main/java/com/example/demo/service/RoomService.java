package com.example.demo.service;

import com.example.demo.entity.Room;
import com.example.demo.entity.RoomStatus;
import com.example.demo.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final Clock clock;

    public RoomService(RoomRepository roomRepository, Clock clock) {
        this.roomRepository = roomRepository;
        this.clock = clock;
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
        Page<UUID> ids = roomRepository.searchAvailableIds(
                query == null ? "" : query.trim(),
                checkIn,
                checkOut,
                guests,
                minPrice,
                maxPrice,
                RoomStatus.AVAILABLE,
                pageable);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        Map<UUID, Integer> order = order(ids.getContent());
        List<Room> rooms = roomRepository.findDetailedByIdIn(ids.getContent()).stream()
                .sorted(Comparator.comparingInt(room -> order.getOrDefault(room.getId(), Integer.MAX_VALUE)))
                .toList();
        return new PageImpl<>(rooms, pageable, ids.getTotalElements());
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
        LocalDate today = LocalDate.now(clock);
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

    private Map<UUID, Integer> order(List<UUID> ids) {
        return ids.stream().collect(Collectors.toMap(Function.identity(), ids::indexOf));
    }
}
