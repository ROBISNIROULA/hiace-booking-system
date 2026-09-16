package com.hiace.hiacebooking.controller;

import com.hiace.hiacebooking.model.Booking;
import com.hiace.hiacebooking.model.Route;
import com.hiace.hiacebooking.repository.BookingRepository;
import com.hiace.hiacebooking.service.BookingService;
import com.hiace.hiacebooking.service.EsewaService;
import com.hiace.hiacebooking.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.constants.PaymentStatus;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/booking")
public class BookingController {

    @Autowired private BookingService    bookingService;
    @Autowired private RouteService      routeService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private EsewaService      esewaService;

    @Value("${esewa.merchant-code}")
    private String merchantCode;

    @Value("${esewa.success-url}")
    private String successUrl;

    @Value("${esewa.failure-url}")
    private String failureUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER — Format amount without trailing .0
    // eSewa rejects "2500.0" but accepts "2500"
    // ─────────────────────────────────────────────────────────────────────────
    private String formatAmount(double amount) {
        if (amount == Math.floor(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEAT SELECTION PAGE
    // GET /booking/select-seat/{routeId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/select-seat/{routeId}")
    public String selectSeat(@PathVariable Long routeId,
                              Model model,
                              RedirectAttributes ra) {

        Route route = routeService.getRouteById(routeId);

        // ── Block booking on a route whose departure date has passed ──
        if (route.getDepartureDate() != null &&
            route.getDepartureDate()
                 .isBefore(java.time.LocalDate.now())) {
            ra.addFlashAttribute("errorMsg",
                "❌ This route has already departed and is " +
                "no longer available for booking.");
            return "redirect:/hiaces";
        }

     // Only treat seats as booked if the booking is
     // CONFIRMED or PENDING — CANCELLED seats go back
     // to the available pool
     List<Integer> bookedSeats = bookingRepository
             .findByRoute(route)
             .stream()
             .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
             .map(Booking::getSeatNumber)
             .toList();

        model.addAttribute("route", route);
        model.addAttribute("bookedSeats", bookedSeats);
        model.addAttribute("autoSeat",
                bookingService.autoAssignSeat(routeId));
        return "seat-selection";
    }
    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM MULTIPLE SEATS → eSewa Payment Page
    // POST /booking/confirm-multiple
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/confirm-multiple")
    public String confirmMultiple(
            @RequestParam Long routeId,
            @RequestParam List<Integer> seatNumbers,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        Route route = routeService.getRouteById(routeId);
        List<Booking> successBookings = new ArrayList<>();
        List<Integer> failedSeats     = new ArrayList<>();

        // Try to book each selected seat individually
        for (int seatNum : seatNumbers) {
            try {
                Booking b = bookingService.createBooking(
                        routeId, seatNum,
                        userDetails.getUsername());
                successBookings.add(b);
            } catch (RuntimeException e) {
                failedSeats.add(seatNum);
            }
        }

        // If ALL seats failed → go back to seat selection
        if (successBookings.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg",
                "❌ Could not process booking. " +
                "Please go back and select seats again.");
            return "redirect:/booking/select-seat/" + routeId;
        }

        // If SOME seats failed → show warning but continue
        if (!failedSeats.isEmpty()) {
            redirectAttributes.addFlashAttribute("warningMsg",
                "⚠️ Seats " + failedSeats +
                " were already taken. " +
                "Proceeding with the remaining seats.");
        }

        // Calculate total using only successfully booked seats
        double totalAmount =
                route.getFare() * successBookings.size();

        // Use first booking ID as the eSewa transaction UUID
        String transactionId = successBookings.get(0).getBookingId()
                + "-" + System.currentTimeMillis();

        // Format amount — removes trailing .0
        String amountStr = formatAmount(totalAmount);

        // Generate HMAC-SHA256 signature for eSewa v2
        String signature = esewaService.generateSignature(
                amountStr, transactionId, merchantCode);

        // Collect the successfully booked seat numbers
        List<Integer> bookedSeatNums = successBookings
                .stream()
                .map(Booking::getSeatNumber)
                .toList();

        // Pass everything to the payment page
        model.addAttribute("amount",       amountStr);
        model.addAttribute("bookingId",    transactionId);
        model.addAttribute("seatCount",    successBookings.size());
        model.addAttribute("seatNumbers",  bookedSeatNums);
        model.addAttribute("route",        route);
        model.addAttribute("merchantCode", merchantCode);
        model.addAttribute("successUrl",   successUrl);
        model.addAttribute("failureUrl",   failureUrl);
        model.addAttribute("signature",    signature);

        return "esewa-payment";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM SINGLE SEAT — used by Auto Assign button
    // POST /booking/confirm
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/confirm")
    public String confirmSingle(
            @RequestParam Long routeId,
            @RequestParam int seatNumber,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Route route = routeService.getRouteById(routeId);

            Booking booking = bookingService.createBooking(
                    routeId, seatNumber,
                    userDetails.getUsername());

            double totalAmount  = route.getFare();
            String transactionId = booking.getBookingId()
                    + "-" + System.currentTimeMillis();


            // Format amount — removes trailing .0
            String amountStr = formatAmount(totalAmount);

            // Generate HMAC-SHA256 signature for eSewa v2
            String signature = esewaService.generateSignature(
                    amountStr, transactionId, merchantCode);

            model.addAttribute("amount",       amountStr);
            model.addAttribute("bookingId",    transactionId);
            model.addAttribute("seatCount",    1);
            model.addAttribute("seatNumbers",  List.of(seatNumber));
            model.addAttribute("route",        route);
            model.addAttribute("merchantCode", merchantCode);
            model.addAttribute("successUrl",   successUrl);
            model.addAttribute("failureUrl",   failureUrl);
            model.addAttribute("signature",    signature);

            return "esewa-payment";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg",
                "❌ Seat " + seatNumber +
                " is already booked! " +
                "Please choose another seat.");
            return "redirect:/booking/select-seat/" + routeId;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ESEWA SUCCESS CALLBACK
    // GET /booking/esewa/success
    // Called by eSewa after successful payment
    // ─────────────────────────────────────────────────────────────────────────
 // ── ESEWA SUCCESS CALLBACK ────────────────────────────────────────────────
    @GetMapping("/esewa/success")
    public String esewaSuccess(
            @RequestParam(required = false) String data,
            @RequestParam(required = false) String oid,
            @RequestParam(required = false) String amt,
            @RequestParam(required = false) String refId,
            Model model) {

        System.out.println("=================================");
        System.out.println("✅ eSewa SUCCESS callback received");
        System.out.println("data param: "   + data);
        System.out.println("oid param: "    + oid);
        System.out.println("amt param: "    + amt);
        System.out.println("refId param: "  + refId);
        System.out.println("=================================");

        String transactionUuid = null;
        String amount          = null;
        String refCode         = null;

        if (data != null && !data.isEmpty()) {
            try {
                byte[] decodedBytes =
                    java.util.Base64.getDecoder().decode(data);
                String jsonStr = new String(decodedBytes);
                System.out.println("Decoded eSewa JSON: " + jsonStr);

                transactionUuid = extractJson(jsonStr,
                                              "transaction_uuid");
                amount          = extractJson(jsonStr,
                                              "total_amount");
                refCode         = extractJson(jsonStr,
                                              "transaction_code");

                System.out.println("Extracted UUID: " + transactionUuid);
                System.out.println("Extracted amount: " + amount);
                System.out.println("Extracted refCode: " + refCode);

            } catch (Exception e) {
                System.out.println("❌ Failed to decode: "
                                   + e.getMessage());
                e.printStackTrace();
            }
        } else if (oid != null) {
            transactionUuid = oid;
            amount          = amt;
            refCode         = refId;
            System.out.println("Using v1 params — oid: "
                               + transactionUuid);
        } else {
            System.out.println("❌ No data received from eSewa!");
        }

        if (transactionUuid != null) {
            System.out.println("Calling confirmPayment with: "
                               + transactionUuid);
            bookingService.confirmPayment(transactionUuid);
        } else {
            System.out.println("❌ transactionUuid is null!" +
                               " Cannot confirm payment.");
        }

        model.addAttribute("refId",  refCode);
        model.addAttribute("amount", amount);
        return "payment-success";
    }
    // Helper — extract value from JSON string without Jackson
    private String extractJson(String json, String key) {
        try {
            // Handle both "key":"value" and "key": "value"
            String search = "\"" + key + "\"";
            int keyIdx = json.indexOf(search);
            if (keyIdx == -1) {
                System.out.println("Key not found in JSON: " + key);
                return null;
            }

            int colonIdx = json.indexOf(":", keyIdx);
            if (colonIdx == -1) return null;

            // Skip whitespace after colon
            int valueStart = colonIdx + 1;
            while (valueStart < json.length() &&
                   (json.charAt(valueStart) == ' ' ||
                    json.charAt(valueStart) == '"')) {
                valueStart++;
            }

            // Find end of value
            int valueEnd = valueStart;
            while (valueEnd < json.length() &&
                   json.charAt(valueEnd) != '"' &&
                   json.charAt(valueEnd) != ',' &&
                   json.charAt(valueEnd) != '}') {
                valueEnd++;
            }

            String result = json.substring(valueStart, valueEnd)
                                .trim()
                                .replace("\"", "");
            System.out.println("Extracted [" + key + "] = " + result);
            return result;

        } catch (Exception e) {
            System.out.println("extractJson error for key '"
                               + key + "': " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ESEWA FAILURE CALLBACK
    // GET /booking/esewa/failure
    // Called by eSewa when payment fails or is cancelled
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/esewa/failure")
    public String esewaFailure(Model model) {
        return "payment-failure";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MY BOOKINGS PAGE
    // GET /booking/my-bookings
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/my-bookings")
    public String myBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        List<Booking> allBookings = bookingService
                .getBookingsSortedByDate(userDetails.getUsername());

        // ── Sort: upcoming confirmed/pending first (by departure date)
        // then past/cancelled at the bottom
        List<Booking> bookings = allBookings.stream()
            .sorted((a, b) -> {
                java.time.LocalDate today =
                    java.time.LocalDate.now();

                java.time.LocalDate dateA =
                    a.getRoute().getDepartureDate();
                java.time.LocalDate dateB =
                    b.getRoute().getDepartureDate();

                boolean aUpcoming = dateA != null &&
                    !dateA.isBefore(today);
                boolean bUpcoming = dateB != null &&
                    !dateB.isBefore(today);

                // Upcoming routes come first
                if (aUpcoming && !bUpcoming) return -1;
                if (!aUpcoming && bUpcoming) return 1;

                // Among upcoming — nearest departure first
                if (aUpcoming) {
                    return dateA.compareTo(dateB);
                }

                // Among past — most recent departure first
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            })
            .collect(java.util.stream.Collectors.toList());

        // Pre-compute counts
        long confirmedCount = bookings.stream()
            .filter(b -> b.getBookingStatus()
                          == BookingStatus.CONFIRMED)
            .count();

        long pendingCount = bookings.stream()
            .filter(b -> b.getBookingStatus()
                          == BookingStatus.PENDING)
            .count();

        long cancelledCount = bookings.stream()
            .filter(b -> b.getBookingStatus()
                          == BookingStatus.CANCELLED)
            .count();

        // Generate eSewa signatures for PENDING bookings
        // so the Pay Due button works directly
        Map<Long, String> esewaSignatureMap = new HashMap<>();
        Map<Long, String> transactionIdMap  = new HashMap<>();

        bookings.stream()
            .filter(b -> b.getPaymentStatus()
                          == PaymentStatus.PENDING)
            .forEach(b -> {
                String txnId = b.getBookingId()
                    + "-" + System.currentTimeMillis();
                String amountStr = formatAmount(
                    b.getRoute().getFare());
                String sig = esewaService.generateSignature(
                    amountStr, txnId, merchantCode);
                esewaSignatureMap.put(b.getBookingId(), sig);
                transactionIdMap.put(b.getBookingId(), txnId);
            });
        
     // Build set of booking IDs that are still cancellable
     // (departure is more than 12 hours away)
     Set<Long> cancellableIds = bookings.stream()
         .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
         .filter(b -> {
             try {
                 if (b.getRoute().getDepartureDate() == null ||
                     b.getRoute().getDepartureTime() == null)
                     return false;

                 LocalDateTime departure = LocalDateTime.of(
                     b.getRoute().getDepartureDate(),
                     java.time.LocalTime.parse(
                         b.getRoute().getDepartureTime())
                 );
                 // Allow cancel only if more than 12 hours remain
                 return departure.isAfter(
                     LocalDateTime.now().plusHours(12));
             } catch (Exception e) {
                 return false;
             }
         })
         .map(b -> b.getBookingId())
         .collect(java.util.stream.Collectors.toSet());

     model.addAttribute("cancellableIds", cancellableIds);
     
  // Build set of booking IDs where departure has passed
  // (trip is completed)
  Set<Long> completedIds = bookings.stream()
      .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
      .filter(b -> {
          try {
              if (b.getRoute().getDepartureDate() == null ||
                  b.getRoute().getDepartureTime() == null)
                  return false;

              LocalDateTime departure = LocalDateTime.of(
                  b.getRoute().getDepartureDate(),
                  java.time.LocalTime.parse(
                      b.getRoute().getDepartureTime())
              );
              // Completed = departure time has already passed
              return departure.isBefore(LocalDateTime.now());
          } catch (Exception e) {
              return false;
          }
      })
      .map(b -> b.getBookingId())
      .collect(java.util.stream.Collectors.toSet());

  model.addAttribute("completedIds", completedIds);

        model.addAttribute("bookings",          bookings);
        model.addAttribute("totalCount",        bookings.size());
        model.addAttribute("confirmedCount",    confirmedCount);
        model.addAttribute("pendingCount",      pendingCount);
        model.addAttribute("cancelledCount",    cancelledCount);
        model.addAttribute("esewaSignatureMap", esewaSignatureMap);
        model.addAttribute("transactionIdMap",  transactionIdMap);
        model.addAttribute("merchantCode",      merchantCode);
        model.addAttribute("successUrl",        successUrl);
        model.addAttribute("failureUrl",        failureUrl);

        return "my-bookings";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL BOOKING
    // POST /booking/cancel/{bookingId}
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return "redirect:/booking/my-bookings";
    }
}