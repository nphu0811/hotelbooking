package com.example.demo.service;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.Review;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ReviewService {
    private static final Set<String> BLOCKED_WORDS = Set.of("spam", "scam", "lừa đảo");

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         RoomRepository roomRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    public List<Review> latestFor(Room room) {
        return reviewRepository.findTop5ByRoomOrderByCreatedAtDesc(room);
    }

    @Transactional
    public Review create(User user, UUID bookingId, int rating, int cleanliness, int service,
                         int location, int value, String content) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt phòng"));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Bạn không có quyền đánh giá đơn này");
        }
        if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
            throw new BusinessException("Chỉ có thể đánh giá sau khi hoàn thành lưu trú");
        }
        if (reviewRepository.existsByBooking(booking)) {
            throw new BusinessException("Bạn đã đánh giá đặt phòng này");
        }
        validateRating(rating, cleanliness, service, location, value);
        String text = content == null ? "" : content.trim();
        if (text.length() < 50 || text.length() > 2000) {
            throw new BusinessException("Nội dung đánh giá phải từ 50 đến 2000 ký tự");
        }
        String lower = text.toLowerCase();
        if (BLOCKED_WORDS.stream().anyMatch(lower::contains)) {
            throw new BusinessException("Nội dung không phù hợp");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setUser(user);
        review.setRoom(booking.getRoom());
        review.setRating(rating);
        review.setCleanlinessRating(cleanliness);
        review.setServiceRating(service);
        review.setLocationRating(location);
        review.setValueRating(value);
        review.setContent(text);
        Review saved = reviewRepository.save(review);
        booking.setReviewed(true);
        bookingRepository.save(booking);
        refreshRoomRating(booking.getRoom());
        return saved;
    }

    private void refreshRoomRating(Room room) {
        List<Review> reviews = reviewRepository.findByRoom(room);
        BigDecimal average = reviews.stream()
                .map(review -> BigDecimal.valueOf(review.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
        room.setAverageRating(average);
        room.setReviewCount(reviews.size());
        roomRepository.save(room);
    }

    private void validateRating(int... ratings) {
        for (int rating : ratings) {
            if (rating < 1 || rating > 5) {
                throw new BusinessException("Điểm đánh giá phải từ 1 đến 5");
            }
        }
    }
}
