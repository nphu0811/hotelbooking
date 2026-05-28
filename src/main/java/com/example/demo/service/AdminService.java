package com.example.demo.service;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomStatus;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final Clock clock;

    public AdminService(RoomRepository roomRepository,
                        HotelRepository hotelRepository,
                        BookingRepository bookingRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        EmailService emailService,
                        Clock clock) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.clock = clock;
    }

    public Page<Room> rooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    public Page<Hotel> hotels(Pageable pageable) {
        return hotelRepository.findAll(pageable);
    }

    public Page<Room> roomsForHotel(UUID hotelId, Pageable pageable) {
        requireHotel(hotelId);
        return roomRepository.findActivePageByHotelId(hotelId, pageable);
    }

    public Hotel requireHotel(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy khách sạn"));
    }

    public long roomCountForHotel(UUID hotelId) {
        Hotel hotel = requireHotel(hotelId);
        return roomRepository.countByHotelAndDeletedFalse(hotel);
    }

    @Transactional
    public Hotel createHotel(User actor, String name, String city, String province, String address,
                             String description, Integer starRating) {
        validateHotelFields(name, city, province, address);
        Hotel hotel = new Hotel();
        hotel.setName(name.trim());
        hotel.setSlug(slug(name));
        hotel.setCity(city.trim());
        hotel.setProvince(province.trim());
        hotel.setAddress(address.trim());
        hotel.setAddressLine(address.trim());
        hotel.setDescription(description == null ? "" : description.trim());
        hotel.setStarRating(starRating);
        hotel.setSource("MANUAL");
        Hotel saved = hotelRepository.save(hotel);
        auditService.record(actor, "CREATE_HOTEL", "HOTEL", saved.getId());
        return saved;
    }

    @Transactional
    public Hotel updateHotel(User actor, UUID hotelId, String name, String city, String province,
                             String address, String description, Integer starRating) {
        Hotel hotel = requireHotel(hotelId);
        if (name != null && !name.isBlank()) {
            hotel.setName(name.trim());
            hotel.setSlug(slug(name));
        }
        if (city != null && !city.isBlank()) {
            hotel.setCity(city.trim());
        }
        if (province != null && !province.isBlank()) {
            hotel.setProvince(province.trim());
        }
        if (address != null && !address.isBlank()) {
            hotel.setAddress(address.trim());
            hotel.setAddressLine(address.trim());
        }
        if (description != null) {
            hotel.setDescription(description.trim());
        }
        if (starRating != null) {
            hotel.setStarRating(starRating);
        }
        Hotel saved = hotelRepository.save(hotel);
        auditService.record(actor, "UPDATE_HOTEL", "HOTEL", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteHotel(User actor, UUID hotelId) {
        Hotel hotel = requireHotel(hotelId);
        hotel.setDeleted(true);
        hotelRepository.save(hotel);
        auditService.record(actor, "DELETE_HOTEL", "HOTEL", hotelId);
    }

    @Transactional
    public Room createRoom(User actor, UUID hotelId, String name, String roomType, int capacity,
                           BigDecimal pricePerNight, String description) {
        Hotel hotel = requireHotel(hotelId);
        if (hotel.isDeleted()) {
            throw new BusinessException("Không thể thêm phòng cho khách sạn đã tắt");
        }
        validateRoomFields(name, roomType, capacity, pricePerNight);
        Room room = new Room();
        room.setHotel(hotel);
        room.setName(name.trim());
        room.setRoomType(roomType.trim());
        room.setCapacity(capacity);
        room.setPricePerNight(pricePerNight);
        room.setDescription(description == null ? "" : description.trim());
        room.setCancellationPolicy("Chính sách hủy theo quy định khách sạn.");
        room.setStatus(RoomStatus.AVAILABLE);
        Room saved = roomRepository.save(room);
        auditService.record(actor, "CREATE_ROOM", "ROOM", saved.getId());
        return saved;
    }

    @Transactional
    public Room updateRoomForHotel(User actor, UUID hotelId, UUID roomId, String name, String roomType,
                                   Integer capacity, BigDecimal price, RoomStatus status, String description) {
        Room room = requireRoomForHotel(hotelId, roomId);
        if (name != null && !name.isBlank()) {
            room.setName(name.trim());
        }
        if (roomType != null && !roomType.isBlank()) {
            room.setRoomType(roomType.trim());
        }
        if (capacity != null) {
            if (capacity < 1 || capacity > 20) {
                throw new BusinessException("Sức chứa phòng không hợp lệ");
            }
            room.setCapacity(capacity);
        }
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
                throw new BusinessException("Giá phòng không hợp lệ");
            }
            room.setPricePerNight(price);
        }
        if (status != null) {
            room.setStatus(status);
        }
        if (description != null) {
            room.setDescription(description.trim());
        }
        Room saved = roomRepository.save(room);
        auditService.record(actor, "UPDATE_ROOM", "ROOM", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteRoomForHotel(User actor, UUID hotelId, UUID roomId) {
        Room room = requireRoomForHotel(hotelId, roomId);
        room.setDeleted(true);
        roomRepository.save(room);
        auditService.record(actor, "DELETE_ROOM", "ROOM", roomId);
    }

    public Room requireRoomForHotel(UUID hotelId, UUID roomId) {
        requireHotel(hotelId);
        return roomRepository.findActiveByHotelIdAndId(hotelId, roomId)
                .orElseThrow(() -> new BusinessException("Phòng không thuộc khách sạn này"));
    }

    private void validateHotelFields(String name, String city, String province, String address) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên khách sạn không được để trống");
        }
        if (city == null || city.isBlank()) {
            throw new BusinessException("Thành phố không được để trống");
        }
        if (province == null || province.isBlank()) {
            throw new BusinessException("Tỉnh/thành không được để trống");
        }
        if (address == null || address.isBlank()) {
            throw new BusinessException("Địa chỉ không được để trống");
        }
    }

    private void validateRoomFields(String name, String roomType, int capacity, BigDecimal price) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên phòng không được để trống");
        }
        if (roomType == null || roomType.isBlank()) {
            throw new BusinessException("Loại phòng không được để trống");
        }
        if (capacity < 1 || capacity > 20) {
            throw new BusinessException("Sức chứa phòng không hợp lệ");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0
                || price.compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
            throw new BusinessException("Giá phòng không hợp lệ");
        }
    }

    private String slug(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    public Page<Booking> bookings(Pageable pageable) {
        return bookingRepository.findAdminPageWithDetails(pageable);
    }

    public Page<User> users(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public DashboardStats dashboardStats() {
        long totalHotels = hotelRepository.countByDeletedFalse();
        long googleHotels = hotelRepository.countBySourceAndDeletedFalse("GOOGLE_PLACES");
        long overpassHotels = hotelRepository.countBySourceAndDeletedFalse("OVERPASS");
        long totalRooms = roomRepository.countByDeletedFalse();
        long availableRooms = roomRepository.countByStatusAndDeletedFalse(RoomStatus.AVAILABLE);
        long paidBookings = bookingRepository.countByStatusIn(List.of(
                BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN,
                BookingStatus.CHECKED_OUT));
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING_PAYMENT);
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long emailVerifiedUsers = userRepository.countByEmailVerifiedTrue();
        long phoneVerifiedUsers = userRepository.countByPhoneVerifiedTrue();
        BigDecimal revenue = bookingRepository.sumTotalAmountByStatusIn(List.of(
                BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN,
                BookingStatus.CHECKED_OUT));
        int occupancySignal = totalRooms == 0 ? 0 : (int) Math.round((paidBookings * 100.0) / totalRooms);
        return new DashboardStats(
                totalHotels,
                googleHotels,
                overpassHotels,
                totalRooms,
                availableRooms,
                paidBookings,
                pendingBookings,
                activeUsers,
                emailVerifiedUsers,
                phoneVerifiedUsers,
                revenue == null ? BigDecimal.ZERO : revenue,
                Math.min(occupancySignal, 100));
    }

    @Transactional
    public Room updateRoom(User actor, UUID roomId, BigDecimal price, RoomStatus status) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng"));
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
                throw new BusinessException("Giá phòng không hợp lệ");
            }
            room.setPricePerNight(price);
        }
        if (status != null) {
            room.setStatus(status);
        }
        Room saved = roomRepository.save(room);
        auditService.record(actor, "UPDATE_ROOM", "ROOM", saved.getId());
        return saved;
    }

    @Transactional
    public Booking checkIn(User actor, UUID bookingId) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy booking"));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Chỉ booking CONFIRMED mới check-in được");
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(Instant.now(clock));
        Booking saved = bookingRepository.save(booking);
        auditService.record(actor, "CHECK_IN", "BOOKING", saved.getId());
        emailService.enqueue(saved.getUser(), saved, EmailEventType.CHECKED_IN,
                saved.getUser().getEmail(), "Chào mừng quý khách đã check-in", "checked-in");
        return saved;
    }

    @Transactional
    public Booking checkOut(User actor, UUID bookingId) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy booking"));
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BusinessException("Chỉ booking CHECKED_IN mới check-out được");
        }
        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckedOutAt(Instant.now(clock));
        Booking saved = bookingRepository.save(booking);
        auditService.record(actor, "CHECK_OUT", "BOOKING", saved.getId());
        emailService.enqueue(saved.getUser(), saved, EmailEventType.CHECKED_OUT,
                saved.getUser().getEmail(), "Xác nhận check-out thành công", "checked-out");
        return saved;
    }

    @Transactional
    public void lockUser(User actor, UUID userId, String reason) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
        boolean superAdmin = target.getRoles().stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getCode()));
        if (superAdmin) {
            throw new BusinessException("Không được phép khóa tài khoản SUPER_ADMIN");
        }
        target.setStatus(UserStatus.LOCKED);
        target.setLockReason(reason);
        userRepository.save(target);
        auditService.record(actor, "LOCK_USER", "USER", target.getId());
    }

    @Transactional
    public void unlockUser(User actor, UUID userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
        target.setStatus(UserStatus.ACTIVE);
        target.setLockReason(null);
        target.setLockedUntil(null);
        userRepository.save(target);
        auditService.record(actor, "UNLOCK_USER", "USER", target.getId());
        emailService.enqueue(target, null, EmailEventType.ACCOUNT_UNLOCKED,
                target.getEmail(), "Tài khoản đã được mở khóa", "account-unlocked");
    }

    public record DashboardStats(
            long totalHotels,
            long googleHotels,
            long overpassHotels,
            long totalRooms,
            long availableRooms,
            long paidBookings,
            long pendingBookings,
            long activeUsers,
            long emailVerifiedUsers,
            long phoneVerifiedUsers,
            BigDecimal revenue,
            int occupancySignal
    ) {
    }
}
