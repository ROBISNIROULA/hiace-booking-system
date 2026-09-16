package com.hiace.hiacebooking.serviceimpl;

import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.model.*;
import com.hiace.hiacebooking.repository.*;
import com.hiace.hiacebooking.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired private ReviewRepository  reviewRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository    userRepository;
    @Autowired private HiaceRepository   hiaceRepository;

    @Override
    public Review addReview(Long bookingId, int rating,
                             String comment, String username) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                    new RuntimeException("Booking not found"));

        User user = userRepository.findByUsername(username);

        // Only confirmed bookings can be reviewed
        if (booking.getBookingStatus()
                   != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                "You can only review confirmed bookings.");
        }

        // Check if already reviewed
        if (reviewRepository.findByUserAndBooking(user, booking)
                            .isPresent()) {
            throw new RuntimeException(
                "You have already reviewed this booking.");
        }

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new RuntimeException(
                "Rating must be between 1 and 5.");
        }

        Hiace hiace = booking.getRoute().getHiace();

        Review review = Review.builder()
                .rating(rating)
                .comment(comment)
                .reviewDate(LocalDateTime.now())
                .user(user)
                .hiace(hiace)
                .booking(booking)
                .build();

        return reviewRepository.save(review);
    }

    @Override
    public List<Review> getReviewsByHiace(Long hiaceId) {
        Hiace hiace = hiaceRepository.findById(hiaceId)
                .orElseThrow();
        return reviewRepository.findByHiace(hiace)
                .stream()
                .sorted((a, b) -> b.getReviewDate()
                                   .compareTo(a.getReviewDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Review> getReviewsByUser(String username) {
        User user = userRepository.findByUsername(username);
        return reviewRepository.findByUser(user);
    }

    @Override
    public double getAverageRating(Long hiaceId) {
        Hiace hiace = hiaceRepository.findById(hiaceId)
                .orElseThrow();
        Double avg = reviewRepository
                .findAverageRatingByHiace(hiace);
        return avg != null ?
               Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public long getReviewCount(Long hiaceId) {
        Hiace hiace = hiaceRepository.findById(hiaceId)
                .orElseThrow();
        Long count = reviewRepository.countByHiace(hiace);
        return count != null ? count : 0L;
    }

    @Override
    public Map<Integer, Long> getRatingDistribution(
            Long hiaceId) {
        Hiace hiace = hiaceRepository.findById(hiaceId)
                .orElseThrow();
        List<Review> reviews =
                reviewRepository.findByHiace(hiace);

        Map<Integer, Long> dist = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            final int star = i;
            dist.put(star, reviews.stream()
                    .filter(r -> r.getRating() == star)
                    .count());
        }
        return dist;
    }

    @Override
    public boolean hasUserReviewedBooking(Long bookingId,
                                           String username) {
        Booking booking = bookingRepository
                .findById(bookingId).orElse(null);
        if (booking == null) return false;
        User user = userRepository.findByUsername(username);
        return reviewRepository
                .findByUserAndBooking(user, booking)
                .isPresent();
    }
}