package com.hiace.hiacebooking.service;

import com.hiace.hiacebooking.model.Review;
import java.util.List;
import java.util.Map;

public interface ReviewService {
    Review addReview(Long bookingId, int rating,
                     String comment, String username);
    List<Review> getReviewsByHiace(Long hiaceId);
    List<Review> getReviewsByUser(String username);
    double getAverageRating(Long hiaceId);
    long getReviewCount(Long hiaceId);
    Map<Integer, Long> getRatingDistribution(Long hiaceId);
    boolean hasUserReviewedBooking(Long bookingId,
                                   String username);
}