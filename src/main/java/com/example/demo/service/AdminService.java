package com.example.demo.service;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.EmailEventType;
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
