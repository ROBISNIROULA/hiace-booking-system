package com.hiace.hiacebooking.controller;

import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.constants.SeatCategory;
import com.hiace.hiacebooking.model.Hiace;
import com.hiace.hiacebooking.model.Route;
import com.hiace.hiacebooking.repository.BookingRepository;
import com.hiace.hiacebooking.repository.HiaceRepository;
import com.hiace.hiacebooking.repository.RouteRepository;
import com.hiace.hiacebooking.service.BookingService;
import com.hiace.hiacebooking.service.ReviewService;
import com.hiace.hiacebooking.service.RouteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hiaces")
public class HiaceController {

    @Autowired private HiaceRepository  hiaceRepository;
    @Autowired private RouteRepository  routeRepository;
    @Autowired private ReviewService    reviewService;
    @Autowired private RouteService routeService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingService bookingService;

    @GetMapping
    public String viewHiaces(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortDate,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            Model model) {

        // Get all routes with hiaces
    	List<Route> routes = routeService.getActiveRoutes();

        // Filter by category
        if (category != null && !category.isEmpty()) {
            routes = routes.stream()
                    .filter(r -> r.getHiace().getCategory()
                                  .name().equals(category))
                    .collect(Collectors.toList());
        }

        // Filter by source
        if (source != null && !source.isEmpty()) {
            String s = source.toLowerCase();
            routes = routes.stream()
                    .filter(r -> r.getSource()
                                  .toLowerCase().contains(s))
                    .collect(Collectors.toList());
        }

        // Filter by destination
        if (destination != null && !destination.isEmpty()) {
            String d = destination.toLowerCase();
            routes = routes.stream()
                    .filter(r -> r.getDestination()
                                  .toLowerCase().contains(d))
                    .collect(Collectors.toList());
        }

        // Sort by date
        if ("nearest".equals(sortDate)) {
            routes = routes.stream()
                    .filter(r -> r.getDepartureDate() != null &&
                                 !r.getDepartureDate()
                                   .isBefore(LocalDate.now()))
                    .sorted(Comparator.comparing(
                        Route::getDepartureDate))
                    .collect(Collectors.toList());
        } else if ("farthest".equals(sortDate)) {
            routes = routes.stream()
                    .filter(r -> r.getDepartureDate() != null)
                    .sorted(Comparator.comparing(
                        Route::getDepartureDate).reversed())
                    .collect(Collectors.toList());
        }

        // Build rating map for each hiace
        Map<Long, Double> ratingMap  = new HashMap<>();
        Map<Long, Long>   countMap   = new HashMap<>();

        Set<Long> hiaceIds = routes.stream()
                .map(r -> r.getHiace().getHiaceId())
                .collect(Collectors.toSet());

        hiaceIds.forEach(hid -> {
            ratingMap.put(hid,
                reviewService.getAverageRating(hid));
            countMap.put(hid,
                reviewService.getReviewCount(hid));
        });
     // ── Most Popular Route (Priority Queue) ──────────────────
        List<Route> popularRoutes = routeService.getRoutesByPriority();

        if (!popularRoutes.isEmpty()) {

            Route popularRoute = popularRoutes.get(0);
            String popSource   = popularRoute.getSource();
            String popDest     = popularRoute.getDestination();

            // Next upcoming scheduled route for this pair
            Optional<Route> nextScheduled = routeService.getActiveRoutes()
                    .stream()
                    .filter(r -> r.getSource().equals(popSource)
                              && r.getDestination().equals(popDest)
                              && r.getDepartureDate() != null
                              && !r.getDepartureDate()
                                    .isBefore(LocalDate.now()))
                    .min(Comparator.comparing(Route::getDepartureDate));

            // Count confirmed bookings ONLY for that specific
            // upcoming route — not historical, not other routes
            long tripCount = 0L;
            if (nextScheduled.isPresent()) {
                tripCount = bookingRepository
                        .findByRoute(nextScheduled.get())
                        .stream()
                        .filter(b -> b.getBookingStatus()
                                      == BookingStatus.CONFIRMED)
                        .count();
            }

            model.addAttribute("popularSource",    popSource);
            model.addAttribute("popularDest",      popDest);
            model.addAttribute("popularNextRoute", nextScheduled.orElse(null));
            model.addAttribute("popularTripCount", tripCount);

        } else {

            model.addAttribute("popularSource",    null);
            model.addAttribute("popularDest",      null);
            model.addAttribute("popularNextRoute", null);
            model.addAttribute("popularTripCount", 0L);

        }
        // Get distinct sources and destinations for dropdowns
        List<String> allSources = routeRepository.findAll()
                .stream()
                .map(Route::getSource)
                .distinct().sorted()
                .collect(Collectors.toList());

        List<String> allDestinations = routeRepository.findAll()
                .stream()
                .map(Route::getDestination)
                .distinct().sorted()
                .collect(Collectors.toList());

        model.addAttribute("routes",          routes);
        model.addAttribute("ratingMap",       ratingMap);
        model.addAttribute("countMap",        countMap);
        model.addAttribute("categories",
            SeatCategory.values());
        model.addAttribute("allSources",      allSources);
        model.addAttribute("allDestinations", allDestinations);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSortDate", sortDate);
        model.addAttribute("selectedSource",   source);
        model.addAttribute("selectedDest",     destination);

        return "view-hiaces";
    }
    @GetMapping("/list")
    public String listAllHiaces(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Model model) {

        List<Hiace> hiaces = hiaceRepository.findAll();

        // Filter by category
        if (category != null && !category.isEmpty()) {
            hiaces = hiaces.stream()
                    .filter(h -> h.getCategory().name().equals(category))
                    .collect(Collectors.toList());
        }

        // Filter by search
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            hiaces = hiaces.stream()
                    .filter(h ->
                        h.getVehicleNumber().toLowerCase().contains(q) ||
                        (h.getDriverName() != null &&
                         h.getDriverName().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        // Build rating map
        Map<Long, Double> ratingMap = new HashMap<>();
        Map<Long, Long>   countMap  = new HashMap<>();
        hiaces.forEach(h -> {
            ratingMap.put(h.getHiaceId(),
                reviewService.getAverageRating(h.getHiaceId()));
            countMap.put(h.getHiaceId(),
                reviewService.getReviewCount(h.getHiaceId()));
        });

        model.addAttribute("hiaces",           hiaces);
        model.addAttribute("ratingMap",        ratingMap);
        model.addAttribute("countMap",         countMap);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSearch",   search);
        model.addAttribute("totalHiaces",      hiaces.size());

        return "hiaces-display";
    }
}