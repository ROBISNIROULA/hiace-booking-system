package com.hiace.hiacebooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.model.Booking;
import com.hiace.hiacebooking.model.Route;
import com.hiace.hiacebooking.model.User;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser(User user);

    List<Booking> findByRoute(Route route);

    Optional<Booking> findByTransactionId(String transactionId);

    List<Booking> findBySeatNumberAndRoute(int seatNumber, Route route);

    // Only return bookings that are NOT cancelled for a specific seat
    List<Booking> findBySeatNumberAndRouteAndBookingStatusNot(
        int seatNumber, Route route, BookingStatus status);

    // Only return active bookings (PENDING or CONFIRMED) for a route
    @Query("SELECT b FROM Booking b WHERE b.route = :route " +
           "AND b.bookingStatus != com.hiace.hiacebooking.constants.BookingStatus.CANCELLED")
    List<Booking> findActiveBookingsByRoute(@Param("route") Route route);
}