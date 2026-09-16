package com.hiace.hiacebooking.controller;

import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.model.Hiace;
import com.hiace.hiacebooking.model.Route;
import com.hiace.hiacebooking.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hiace.hiacebooking.repository.BookingRepository;
import com.hiace.hiacebooking.repository.HiaceRepository;
import com.hiace.hiacebooking.service.ReviewService;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private RouteService routeService;
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private HiaceRepository hiaceRepository;
    
    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/home")
    public String home(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String sortBy,
            Model model) {

        // ── Routes for stats counter ──────────────────────────────
        List<Route> routes;
        if (destination != null && !destination.trim().isEmpty()) {
            routes = routeService.searchRoutes(destination.trim());
            model.addAttribute("searchQuery", destination);
        } else if ("fare".equals(sortBy)) {
            routes = routeService.getRoutesSortedByFare();
        } else {
            routes = routeService.getRoutesSortedByDate();
        }
        
        // Build rating map for each hiace on the home page
        Map<Long, Double> ratingMap = new HashMap<>();
        Map<Long, Long>   countMap  = new HashMap<>();

        routes.forEach(r -> {
            Long hid = r.getHiace().getHiaceId();
            if (!ratingMap.containsKey(hid)) {
                ratingMap.put(hid,
                    reviewService.getAverageRating(hid));
                countMap.put(hid,
                    reviewService.getReviewCount(hid));
            }
        });
        
        // ── ALL HIACES for the fleet gallery ─────────────────────
        // This is the key fix — fetch directly from hiaceRepository
        List<Hiace> allHiaces = hiaceRepository.findAll();

        System.out.println("HomeController — allHiaces count: "
                           + allHiaces.size());

        // ── Happy passenger count ─────────────────────────────────
        long happyPassengerCount = bookingRepository.findAll()
                .stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CONFIRMED)
                .map(b -> b.getUser().getUserId())
                .distinct()
                .count();

        // ── Pass everything to the template ──────────────────────
        model.addAttribute("routes",             routes);
        model.addAttribute("ratingMap",          ratingMap);
        model.addAttribute("countMap",           countMap);
        model.addAttribute("allHiaces",          allHiaces);
        model.addAttribute("happyPassengerCount", happyPassengerCount);
        model.addAttribute("sortBy",
            sortBy != null ? sortBy : "date");
        
        return "Home";
    }
}