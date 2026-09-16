package com.hiace.hiacebooking.controller;

import com.hiace.hiacebooking.model.Review;
import com.hiace.hiacebooking.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // Show review form for a booking
    @GetMapping("/add/{bookingId}")
    public String showReviewForm(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes ra) {

        // Check if already reviewed
        if (reviewService.hasUserReviewedBooking(
                bookingId,
                userDetails.getUsername())) {
            ra.addFlashAttribute("errorMsg",
                "You have already reviewed this booking.");
            return "redirect:/booking/my-bookings";
        }

        model.addAttribute("bookingId", bookingId);
        return "review-form";
    }

    // Submit review
    @PostMapping("/submit")
    public String submitReview(
            @RequestParam Long bookingId,
            @RequestParam int rating,
            @RequestParam String comment,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            reviewService.addReview(
                bookingId, rating, comment,
                userDetails.getUsername());
            ra.addFlashAttribute("successMsg",
                "✅ Thank you for your review!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg",
                "❌ " + e.getMessage());
        }
        return "redirect:/booking/my-bookings";
    }

    // View all reviews for a hiace
    @GetMapping("/hiace/{hiaceId}")
    public String viewHiaceReviews(
            @PathVariable Long hiaceId,
            Model model) {

        List<Review> reviews =
            reviewService.getReviewsByHiace(hiaceId);
        double avgRating =
            reviewService.getAverageRating(hiaceId);
        long reviewCount =
            reviewService.getReviewCount(hiaceId);

        model.addAttribute("reviews",     reviews);
        model.addAttribute("avgRating",   avgRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("distribution",
            reviewService.getRatingDistribution(hiaceId));
        model.addAttribute("hiaceId",     hiaceId);

        return "hiace-reviews";
    }
}