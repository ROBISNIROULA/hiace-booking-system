package com.hiace.hiacebooking.controller;

import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.constants.PaymentStatus;
import com.hiace.hiacebooking.model.Booking;
import com.hiace.hiacebooking.model.Hiace;
import com.hiace.hiacebooking.model.Route;
import com.hiace.hiacebooking.model.User;
import com.hiace.hiacebooking.repository.BookingRepository;
import com.hiace.hiacebooking.repository.HiaceRepository;
import com.hiace.hiacebooking.repository.RouteRepository;
import com.hiace.hiacebooking.repository.UserRepository;
import com.hiace.hiacebooking.service.ReviewService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private HiaceRepository   hiaceRepository;
    @Autowired private RouteRepository   routeRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository    userRepository;
    @Autowired private ReviewService     reviewService;

    private final String UPLOAD_DIR =
        System.getProperty("user.dir") +
        "/src/main/resources/static/uploads/";

    private final String SERVE_DIR =
        System.getProperty("user.dir") +
        "/target/classes/static/uploads/";

    // ─────────────────────────────────────────────────────────
    //  DASHBOARD
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // ── Hiaces ───────────────────────────────────────────
        long hiaceCount = hiaceRepository.count();

        // ── Users (ROLE_USER only) ────────────────────────────
        // FIX 1: use toString() instead of name() — works for
        // both String fields and Enum fields
        long userCount = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() != null &&
                             u.getRole().toString()
                              .equals("ROLE_USER"))
                .count();

        // ── All bookings ─────────────────────────────────────
        List<Booking> allBookings = bookingRepository.findAll();
        long totalBookings     = allBookings.size();
        long confirmedBookings = allBookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CONFIRMED)
                .count();
        long pendingCount      = allBookings.stream()
                .filter(b -> b.getPaymentStatus()
                              == PaymentStatus.PENDING
                          && b.getBookingStatus()
                              != BookingStatus.CANCELLED)
                .count();
        long cancelledBookings = allBookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CANCELLED)
                .count();

        // ── Income ───────────────────────────────────────────
        double totalIncome = allBookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CONFIRMED)
                .mapToDouble(b -> b.getRoute().getFare())
                .sum();

        LocalDate today = LocalDate.now();

        double todayIncome = allBookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CONFIRMED
                          && b.getBookingDate() != null
                          && b.getBookingDate().equals(today))
                .mapToDouble(b -> b.getRoute().getFare())
                .sum();

        double monthIncome = allBookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CONFIRMED
                          && b.getBookingDate() != null
                          && b.getBookingDate().getYear()
                              == today.getYear()
                          && b.getBookingDate().getMonth()
                              == today.getMonth())
                .mapToDouble(b -> b.getRoute().getFare())
                .sum();

        // ── Popular Route (Priority Queue — inline) ───────────
        // FIX 2: calculate inline without bookingService dependency
        // This avoids the getRoutesByPriority() compilation error
        String popularRoute        = null;
        long   popularRouteBookings = 0L;

        Map<String, Long> routeBookingCount = new HashMap<>();
        allBookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CONFIRMED)
                .forEach(b -> {
                    String key = b.getRoute().getSource()
                               + " → "
                               + b.getRoute().getDestination();
                    routeBookingCount.merge(key, 1L, Long::sum);
                });

        if (!routeBookingCount.isEmpty()) {
            Map.Entry<String, Long> topEntry =
                routeBookingCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (topEntry != null) {
                popularRoute         = topEntry.getKey();
                popularRouteBookings = topEntry.getValue();
            }
        }

        model.addAttribute("popularRoute",         popularRoute);
        model.addAttribute("popularRouteBookings", popularRouteBookings);

        // ── Upcoming trips (today or future, nearest first, top 5)
        List<Route> upcomingTrips = routeRepository.findAll()
                .stream()
                .filter(r -> r.getDepartureDate() != null
                          && !r.getDepartureDate().isBefore(today))
                .sorted(Comparator.comparing(Route::getDepartureDate))
                .limit(5)
                .collect(Collectors.toList());

        long upcomingCount = routeRepository.findAll()
                .stream()
                .filter(r -> r.getDepartureDate() != null
                          && !r.getDepartureDate().isBefore(today))
                .count();

        // Non-cancelled booked seat count per upcoming trip
        Map<Long, Long> tripBookingMap = new HashMap<>();
        upcomingTrips.forEach(r -> {
            long count = bookingRepository.findByRoute(r)
                    .stream()
                    .filter(b -> b.getBookingStatus()
                                  != BookingStatus.CANCELLED)
                    .count();
            tripBookingMap.put(r.getRouteId(), count);
        });

        // ── Recent bookings (latest 8) ─────────────────────────
        List<Booking> recentBookings = allBookings.stream()
                .sorted((a, b) -> {
                    if (a.getBookingDate() == null) return 1;
                    if (b.getBookingDate() == null) return -1;
                    return b.getBookingDate()
                             .compareTo(a.getBookingDate());
                })
                .limit(8)
                .collect(Collectors.toList());

        // ── Pass to model ──────────────────────────────────────
        model.addAttribute("hiaceCount",        hiaceCount);
        model.addAttribute("userCount",         userCount);
        model.addAttribute("totalBookings",     totalBookings);
        model.addAttribute("confirmedBookings", confirmedBookings);
        model.addAttribute("pendingCount",      pendingCount);
        model.addAttribute("cancelledBookings", cancelledBookings);
        model.addAttribute("totalIncome",       (long) totalIncome);
        model.addAttribute("todayIncome",       (long) todayIncome);
        model.addAttribute("monthIncome",       (long) monthIncome);
        model.addAttribute("upcomingTrips",     upcomingTrips);
        model.addAttribute("upcomingCount",     upcomingCount);
        model.addAttribute("tripBookingMap",    tripBookingMap);
        model.addAttribute("recentBookings",    recentBookings);

        return "/admin/dashboard";
    }

    // ─────────────────────────────────────────────────────────
    //  UPCOMING TRIPS (full list page)
    // ─────────────────────────────────────────────────────────
    @GetMapping("/upcoming-trips")
    public String upcomingTrips(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String fromDate,
            Model model) {

        LocalDate today = LocalDate.now();

        List<Route> upcomingTrips = routeRepository.findAll()
                .stream()
                .filter(r -> r.getDepartureDate() != null
                          && !r.getDepartureDate().isBefore(today))
                .sorted(Comparator.comparing(Route::getDepartureDate))
                .collect(Collectors.toList());

        if (source != null && !source.trim().isEmpty()) {
            String s = source.trim().toLowerCase();
            upcomingTrips = upcomingTrips.stream()
                    .filter(r -> r.getSource()
                                  .toLowerCase().contains(s))
                    .collect(Collectors.toList());
        }
        if (destination != null && !destination.trim().isEmpty()) {
            String d = destination.trim().toLowerCase();
            upcomingTrips = upcomingTrips.stream()
                    .filter(r -> r.getDestination()
                                  .toLowerCase().contains(d))
                    .collect(Collectors.toList());
        }
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            try {
                LocalDate from = LocalDate.parse(fromDate.trim());
                upcomingTrips = upcomingTrips.stream()
                        .filter(r -> !r.getDepartureDate()
                                       .isBefore(from))
                        .collect(Collectors.toList());
            } catch (Exception ignored) {}
        }

        Map<Long, Long> tripBookingMap = new HashMap<>();
        upcomingTrips.forEach(r -> {
            long count = bookingRepository.findByRoute(r)
                    .stream()
                    .filter(b -> b.getBookingStatus()
                                  != BookingStatus.CANCELLED)
                    .count();
            tripBookingMap.put(r.getRouteId(), count);
        });

        model.addAttribute("upcomingTrips",  upcomingTrips);
        model.addAttribute("tripBookingMap", tripBookingMap);
        model.addAttribute("upcomingCount",  upcomingTrips.size());

        return "/admin/upcoming-trips";
    }

    // ─────────────────────────────────────────────────────────
    //  TRIP DETAIL
    // ─────────────────────────────────────────────────────────
    @GetMapping("/trip-detail/{routeId}")
    public String tripDetail(@PathVariable Long routeId,
                              Model model) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow();

        // All non-cancelled bookings for this route
        List<Booking> bookings = bookingRepository
                .findByRoute(route)
                .stream()
                .filter(b -> b.getBookingStatus()
                              != BookingStatus.CANCELLED)
                .sorted(Comparator.comparingInt(
                    Booking::getSeatNumber))
                .collect(Collectors.toList());

        long confirmedCount = bookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CONFIRMED)
                .count();

        long pendingCount = bookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.PENDING)
                .count();

        int availableSeats =
                route.getHiace().getTotalSeats()
                - (int) bookings.size();

        // ── Build seatStatusMap ──────────────────────────────
        // Maps every seat number (1-15) to its status string.
        // null = available, "CONFIRMED" or "PENDING" = booked.
        Map<Integer, String> seatStatusMap = new HashMap<>();

        // Pre-fill all seats as available (null)
        for (int i = 1; i <= route.getHiace().getTotalSeats(); i++) {
            seatStatusMap.put(i, null);
        }

        // Override with actual booking statuses
        bookings.forEach(b -> seatStatusMap.put(
            b.getSeatNumber(),
            b.getBookingStatus().toString()
        ));

        model.addAttribute("route",          route);
        model.addAttribute("bookings",       bookings);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("pendingCount",   pendingCount);
        model.addAttribute("availableSeats", availableSeats);
        model.addAttribute("seatStatusMap",  seatStatusMap);

        return "/admin/trip-detail";
    }

    // ─────────────────────────────────────────────────────────
    //  MANAGE HIACES
    // ─────────────────────────────────────────────────────────
    @GetMapping("/hiaces")
    public String manageHiaces(Model model) {
        model.addAttribute("hiaces", hiaceRepository.findAll());
        model.addAttribute("hiace",  new Hiace());
        return "/admin/manage-hiace";
    }

    @PostMapping("/hiaces/save")
    public String saveHiace(
            @ModelAttribute Hiace hiace,
            @RequestParam(value = "image",
                          required = false) MultipartFile image,
            RedirectAttributes ra) {
        try {
            if (image != null && !image.isEmpty()) {
                new File(UPLOAD_DIR).mkdirs();
                new File(SERVE_DIR).mkdirs();

                if (hiace.getImageName() != null) {
                    new File(UPLOAD_DIR +
                        hiace.getImageName()).delete();
                    new File(SERVE_DIR  +
                        hiace.getImageName()).delete();
                }

                String filename = System.currentTimeMillis()
                    + "_" + image.getOriginalFilename()
                                 .replaceAll("\\s+", "_");

                Files.copy(image.getInputStream(),
                    Paths.get(UPLOAD_DIR + filename),
                    StandardCopyOption.REPLACE_EXISTING);
                Files.copy(image.getInputStream(),
                    Paths.get(SERVE_DIR + filename),
                    StandardCopyOption.REPLACE_EXISTING);

                hiace.setImageName(filename);
            }
            hiaceRepository.save(hiace);
            ra.addFlashAttribute("successMessage",
                "✅ Hiace saved successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                "❌ Error: " + e.getMessage());
        }
        return "redirect:/admin/hiaces";
    }

    @GetMapping("/hiaces/delete/{id}")
    public String deleteHiace(@PathVariable Long id,
                               RedirectAttributes ra) {
        try {
            Hiace hiace = hiaceRepository.findById(id)
                    .orElseThrow();

            List<Route> routes =
                routeRepository.findByHiace(hiace);
            routes.forEach(r -> bookingRepository.deleteAll(
                bookingRepository.findByRoute(r)));
            routeRepository.deleteAll(routes);

            if (hiace.getImageName() != null) {
                new File(UPLOAD_DIR +
                    hiace.getImageName()).delete();
                new File(SERVE_DIR  +
                    hiace.getImageName()).delete();
            }
            hiaceRepository.delete(hiace);
            ra.addFlashAttribute("successMessage",
                "✅ Hiace deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                "❌ Error: " + e.getMessage());
        }
        return "redirect:/admin/hiaces";
    }

    // ─────────────────────────────────────────────────────────
    //  MANAGE ROUTES
    // ─────────────────────────────────────────────────────────
    @GetMapping("/routes")
    public String manageRoutes(Model model) {
        LocalDate today = LocalDate.now();

        List<Route> allRoutes = routeRepository.findAll()
                .stream()
                .filter(r -> r.getDepartureDate() != null &&
                            !r.getDepartureDate().isBefore(today))
                .sorted(Comparator.comparing(Route::getDepartureDate))
                .collect(Collectors.toList());
        model.addAttribute("routes", allRoutes);
        model.addAttribute("hiaces", hiaceRepository.findAll());
        model.addAttribute("route",  new Route());
        model.addAttribute("today",  LocalDate.now().toString());
        return "/admin/manage-routes";
    }

    @PostMapping("/routes/save")
    public String saveRoute(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam double fare,
            @RequestParam String departureTime,
            @RequestParam String departureDate,
            @RequestParam Long hiaceId,
            @RequestParam(required = false) Long routeId,
            RedirectAttributes ra) {
        try {
            LocalDate parsed = LocalDate.parse(departureDate);

            if (parsed.isBefore(LocalDate.now())) {
                ra.addFlashAttribute("errorMessage",
                    "❌ Departure date cannot be in the past.");
                return "redirect:/admin/routes";
            }

            Route route = (routeId != null)
                ? routeRepository.findById(routeId)
                                 .orElse(new Route())
                : new Route();

            Hiace hiace = hiaceRepository.findById(hiaceId)
                    .orElseThrow();

            route.setSource(source);
            route.setDestination(destination);
            route.setFare(fare);
            route.setDepartureTime(departureTime);
            route.setDepartureDate(parsed);
            route.setHiace(hiace);

            routeRepository.save(route);
            ra.addFlashAttribute("successMessage",
                routeId != null ?
                "✅ Route updated!" : "✅ Route added!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                "❌ Error: " + e.getMessage());
        }
        return "redirect:/admin/routes";
    }

    @GetMapping("/routes/delete/{id}")
    public String deleteRoute(@PathVariable Long id,
                               RedirectAttributes ra) {
        try {
            Route route = routeRepository.findById(id)
                    .orElseThrow();
            bookingRepository.deleteAll(
                bookingRepository.findByRoute(route));
            routeRepository.delete(route);
            ra.addFlashAttribute("successMessage",
                "✅ Route deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                "❌ Error: " + e.getMessage());
        }
        return "redirect:/admin/routes";
    }

    // ─────────────────────────────────────────────────────────
    //  VIEW BOOKINGS
    // ─────────────────────────────────────────────────────────
    @GetMapping("/bookings")
    public String viewBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {

        List<Booking> bookings = bookingRepository.findAll()
                .stream()
                .sorted((a, b) -> {
                    if (a.getBookingDate() == null) return 1;
                    if (b.getBookingDate() == null) return -1;
                    return b.getBookingDate()
                             .compareTo(a.getBookingDate());
                })
                .collect(Collectors.toList());

        if (status != null && !status.isEmpty()) {
            try {
                BookingStatus bs =
                    BookingStatus.valueOf(status.toUpperCase());
                bookings = bookings.stream()
                        .filter(b -> b.getBookingStatus() == bs)
                        .collect(Collectors.toList());
            } catch (Exception ignored) {}
        }

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            bookings = bookings.stream()
                    .filter(b ->
                        b.getUser().getFirstname()
                         .toLowerCase().contains(q) ||
                        b.getUser().getLastname()
                         .toLowerCase().contains(q) ||
                        (b.getUser().getPhone() != null &&
                         b.getUser().getPhone()
                          .toLowerCase().contains(q)) ||
                        b.getRoute().getSource()
                         .toLowerCase().contains(q) ||
                        b.getRoute().getDestination()
                         .toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        long confirmedCount = bookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CONFIRMED).count();
        long pendingCount   = bookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.PENDING).count();
        long cancelledCount = bookings.stream()
                .filter(b -> b.getBookingStatus()
                              == BookingStatus.CANCELLED).count();

        model.addAttribute("bookings",       bookings);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("pendingCount",   pendingCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("searchQuery",    search);

        return "/admin/view-bookings";
    }

    // ─────────────────────────────────────────────────────────
    //  VIEW USERS
    // ─────────────────────────────────────────────────────────
    @GetMapping("/users")
    public String viewUsers(Model model) {
        // FIX: use toString() — safe for both String and Enum role
        List<User> users = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() != null &&
                             u.getRole().toString()
                              .equals("ROLE_USER"))
                .collect(Collectors.toList());
        model.addAttribute("users", users);
        return "/admin/view-users";
    }
}