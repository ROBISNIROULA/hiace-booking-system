package com.hiace.hiacebooking.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.constants.PaymentStatus;
import com.hiace.hiacebooking.model.Booking;
import com.hiace.hiacebooking.model.Route;
import com.hiace.hiacebooking.model.User;
import com.hiace.hiacebooking.repository.BookingRepository;
import com.hiace.hiacebooking.repository.RouteRepository;
import com.hiace.hiacebooking.repository.SeatRepository;
import com.hiace.hiacebooking.repository.UserRepository;
import com.hiace.hiacebooking.service.BookingService;
import com.hiace.hiacebooking.service.EmailService;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE BOOKING
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public Booking createBooking(Long routeId, int seatNumber,
                                  String username) {

        Route route = routeRepository.findById(routeId).orElseThrow();
        User user = userRepository.findByUsername(username);

        List<Booking> existingBookings = bookingRepository
                .findBySeatNumberAndRouteAndBookingStatusNot(
                    seatNumber, route, BookingStatus.CANCELLED);

        for (Booking existing : existingBookings) {

            String existingUsername = existing.getUser().getUsername();
            BookingStatus status = existing.getBookingStatus();

            // Case 1: Same user has a PENDING booking → reuse it
            if (existingUsername.equals(username) &&
                status == BookingStatus.PENDING) {
                System.out.println("♻️ Reusing PENDING booking #"
                    + existing.getBookingId()
                    + " for user: " + username);
                return existing;
            }

            // Case 2: Same user has CONFIRMED → reuse it
            if (existingUsername.equals(username) &&
                status == BookingStatus.CONFIRMED) {
                System.out.println("♻️ Seat already CONFIRMED by"
                    + " same user — reusing booking #"
                    + existing.getBookingId());
                return existing;
            }

            // Case 3: Different user has CONFIRMED → seat taken
            if (!existingUsername.equals(username) &&
                status == BookingStatus.CONFIRMED) {
                throw new RuntimeException(
                    "Seat " + seatNumber +
                    " is already confirmed by another user!");
            }

            // Case 4: Different user has PENDING → still available
            // (their payment not completed — treat as available)
        }

        // No blocking booking found — create new PENDING booking
        Booking newBooking = Booking.builder()
                .seatNumber(seatNumber)
                .bookingDate(LocalDateTime.now())
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .user(user)
                .route(route)
                .build();

        Booking saved = bookingRepository.save(newBooking);

        System.out.println("✅ New booking created: #"
            + saved.getBookingId()
            + " | Seat " + seatNumber
            + " | User: " + username);

        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTO ASSIGN SEAT
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public int autoAssignSeat(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow();

        // Exclude CANCELLED bookings — those seats
        // are available again
        List<Integer> booked = bookingRepository
                .findByRoute(route)
                .stream()
                .filter(b -> b.getBookingStatus()
                              != BookingStatus.CANCELLED)
                .map(Booking::getSeatNumber)
                .collect(Collectors.toList());

        // Greedy — pick lowest available seat number
        for (int i = 1; i <= route.getHiace().getTotalSeats(); i++) {
            if (!booked.contains(i)) return i;
        }
        return -1; // all seats taken
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BOOKINGS BY USERNAME
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<Booking> getBookingByUsername(String username) {
        User user = userRepository.findByUsername(username);
        return bookingRepository.findByUser(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL BOOKINGS
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BOOKING BY ID
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM PAYMENT + SEND EMAIL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void confirmPayment(String transactionId) {

        System.out.println("=================================");
        System.out.println("confirmPayment called with: "
                           + transactionId);

        if (transactionId == null || transactionId.trim().isEmpty()) {
            System.out.println("❌ transactionId is null/empty!");
            return;
        }

        // Extract booking ID from "bookingId-timestamp" format
        String bookingIdStr;
        if (transactionId.contains("-")) {
            bookingIdStr = transactionId.split("-")[0];
            System.out.println("Extracted bookingId: " + bookingIdStr);
        } else {
            bookingIdStr = transactionId;
        }

        try {
            Long bookingId = Long.parseLong(bookingIdStr.trim());
            System.out.println("Looking for booking #" + bookingId);

            var bookingOpt = bookingRepository.findById(bookingId);

            if (bookingOpt.isEmpty()) {
                System.out.println("❌ Booking #" + bookingId
                                   + " NOT FOUND in database!");
                return;
            }

            Booking booking = bookingOpt.get();
            System.out.println("Found booking: #" + bookingId
                + " | Status: " + booking.getBookingStatus()
                + " | User: " + booking.getUser().getEmail());

            // Update payment status
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            booking.setTransactionId(transactionId);
            bookingRepository.save(booking);
            System.out.println("✅ Booking #" + bookingId
                               + " updated to CONFIRMED");

            // Send confirmation email
            String userEmail    = booking.getUser().getEmail();
            String userName     = booking.getUser().getFirstname();
            String source       = booking.getRoute().getSource();
            String destination  = booking.getRoute().getDestination();
            int    seatNumber   = booking.getSeatNumber();
            double fare         = booking.getRoute().getFare();
            String bookingDate  = booking.getBookingDate()
                                         .toLocalDate().toString();

            System.out.println("Sending email to: " + userEmail);

            emailService.sendPaymentConfirmation(
                userEmail, userName, source, destination,
                seatNumber, fare, transactionId, bookingDate
            );

            System.out.println("=================================");

        } catch (NumberFormatException e) {
            System.out.println("❌ Cannot parse booking ID: "
                               + bookingIdStr);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("❌ Unexpected error in confirmPayment: "
                               + e.getMessage());
            e.printStackTrace();
        }
    }
    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL BOOKING + SEND EMAIL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void cancelBooking(Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            try {
                emailService.sendCancellationNotification(
                    booking.getUser().getEmail(),
                    booking.getUser().getFirstname(),
                    booking.getRoute().getSource(),
                    booking.getRoute().getDestination(),
                    booking.getSeatNumber()
                );
            } catch (Exception e) {
                System.out.println("⚠️ Cancellation email failed: "
                    + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BOOKINGS SORTED BY DATE (Merge Sort)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<Booking> getBookingsSortedByDate(String username) {
        User user = userRepository.findByUsername(username);
        List<Booking> bookings = new ArrayList<>(
                bookingRepository.findByUser(user));
        mergeSort(bookings, 0, bookings.size() - 1);
        return bookings;
    }

    private void mergeSort(List<Booking> list, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(list, left, mid);
            mergeSort(list, mid + 1, right);
            merge(list, left, mid, right);
        }
    }

    private void merge(List<Booking> list, int left, int mid, int right) {
        List<Booking> leftList  = new ArrayList<>(list.subList(left, mid + 1));
        List<Booking> rightList = new ArrayList<>(list.subList(mid + 1, right + 1));
        int i = 0, j = 0, k = left;
        while (i < leftList.size() && j < rightList.size()) {
            if (!leftList.get(i).getBookingDate()
                    .isAfter(rightList.get(j).getBookingDate())) {
                list.set(k++, leftList.get(i++));
            } else {
                list.set(k++, rightList.get(j++));
            }
        }
        while (i < leftList.size())  list.set(k++, leftList.get(i++));
        while (j < rightList.size()) list.set(k++, rightList.get(j++));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALCULATE FARE PRIORITY
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public double calculateFarePriority(Long routeId) {
        Route route = routeRepository.findById(routeId).orElseThrow();
        int bookedCount = bookingRepository.findByRoute(route).size();
        int totalSeats  = route.getHiace().getTotalSeats();

        double occupancyRate = (double) bookedCount / totalSeats;
        return occupancyRate * route.getFare();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ROUTES BY PRIORITY (Priority Queue)
    // ─────────────────────────────────────────────────────────────────────────
    public List<Route> getRoutesByPriority() {
        List<Route> routes = routeRepository.findAll();
        PriorityQueue<Route> pq = new PriorityQueue<>(
            (a, b) -> Double.compare(
                calculateFarePriority(b.getRouteId()),
                calculateFarePriority(a.getRouteId())
            )
        );
        pq.addAll(routes);
        List<Route> sorted = new ArrayList<>();
        while (!pq.isEmpty()) sorted.add(pq.poll());
        return sorted;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCHEDULED — Clean up stale PENDING bookings every 30 minutes
    // ─────────────────────────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 1800000)
    public void cleanupStalePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        List<Booking> stale = bookingRepository.findAll()
                .stream()
                .filter(b ->
                    b.getBookingStatus() == BookingStatus.PENDING &&
                    b.getBookingDate().isBefore(cutoff))
                .toList();

        stale.forEach(b -> {
            b.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(b);
        });

        if (!stale.isEmpty()) {
            System.out.println("🧹 Cleaned up " + stale.size()
                + " stale PENDING bookings");
        }
    }

}