package com.example.demo.service;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.RefundRequest;
import com.example.demo.entity.RefundStatus;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RefundRequestRepository;
import com.example.demo.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingService {
    private static final Set<BookingStatus> ACTIVE_STATUSES = Set.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          PaymentRepository paymentRepository,
                          RefundRequestRepository refundRequestRepository,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.paymentRepository = paymentRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Booking createPendingBooking(User user, UUID roomId, LocalDate checkIn, LocalDate checkOut,
                                        int guests, String specialRequest) {
        validateBookingInput(checkIn, checkOut, guests);
        Room room = roomRepository.lockById(roomId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng"));
        if (room.isDeleted() || room.getStatus() != RoomStatus.AVAILABLE) {
            throw new BusinessException("Phòng tạm thời không nhận đặt");
        }
        long overlaps = bookingRepository.countOverlaps(room, checkIn, checkOut, ACTIVE_STATUSES);
        if (overlaps > 0) {
            throw new BusinessException("Rất tiếc, phòng vừa được đặt bởi người khác");
        }
        int nights = Math.toIntExact(ChronoUnit.DAYS.between(checkIn, checkOut));
        BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Booking booking = new Booking();
        booking.setBookingCode("HB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setGuestCount(guests);
        booking.setPricePerNightSnapshot(room.getPricePerNight());
        booking.setNights(nights);
        booking.setTotalAmount(total);
        booking.setSpecialRequest(specialRequest == null ? null : specialRequest.trim());
        booking.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        return bookingRepository.save(booking);
    }

    public Page<Booking> history(User user, Pageable pageable) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public Booking requireOwnBooking(User user, UUID bookingId) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt phòng"));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Bạn không có quyền truy cập đơn này");
        }
        return booking;
    }

    public Booking requireBooking(UUID bookingId) {
        return bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt phòng"));
    }

    @Transactional
    public RefundRequest cancel(User user, UUID bookingId) {
        Booking booking = requireOwnBooking(user, bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return refundRequestRepository.findByIdempotencyKey(booking.getId().toString())
                    .orElseThrow(() -> new BusinessException("Đơn đã hủy nhưng thiếu refund request"));
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Đơn này không thể hủy");
        }
        if (booking.getCheckIn().equals(LocalDate.now()) && LocalTime.now().isAfter(LocalTime.of(14, 0))) {
            throw new BusinessException("Không thể hủy sau giờ check-in");
        }
        int percentage = refundPercentage(booking.getCheckIn());
        BigDecimal refundAmount = booking.getTotalAmount()
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        Payment payment = paymentRepository.findFirstByBookingOrderByCreatedAtDesc(booking).orElse(null);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        RefundRequest refund = new RefundRequest();
        refund.setBooking(booking);
        refund.setPayment(payment);
        refund.setAmount(refundAmount);
        refund.setPercentage(percentage);
        refund.setStatus(refundAmount.compareTo(BigDecimal.ZERO) > 0 ? RefundStatus.PROCESSING : RefundStatus.SUCCESS);
        refund.setIdempotencyKey(booking.getId().toString());
        RefundRequest saved = refundRequestRepository.save(refund);
        emailService.enqueue(user, booking, EmailEventType.BOOKING_CANCELLED, user.getEmail(),
                "Xác nhận hủy đặt phòng " + booking.getBookingCode(), "booking-cancelled");
        return saved;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingBookings() {
        List<Booking> expired = bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.PENDING_PAYMENT, Instant.now());
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            paymentRepository.findFirstByBookingOrderByCreatedAtDesc(booking).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.INITIATED) {
                    payment.setStatus(PaymentStatus.TIMEOUT);
                    paymentRepository.save(payment);
                }
            });
        }
    }

    private void validateBookingInput(LocalDate checkIn, LocalDate checkOut, int guests) {
        LocalDate today = LocalDate.now();
        if (checkIn == null || checkIn.isBefore(today)) {
            throw new BusinessException("Ngày nhận phòng không hợp lệ");
        }
        if (checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessException("Ngày trả phòng phải sau ngày nhận phòng");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1 || nights > 30) {
            throw new BusinessException("Thời gian lưu trú tối đa 30 đêm");
        }
        if (guests < 1 || guests > 10) {
            throw new BusinessException("Số lượng khách phải từ 1 đến 10 người");
        }
    }

    private int refundPercentage(LocalDate checkIn) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), checkIn);
        if (days >= 3) {
            return 100;
        }
        if (days >= 1) {
            return 50;
        }
        return 0;
    }
}
