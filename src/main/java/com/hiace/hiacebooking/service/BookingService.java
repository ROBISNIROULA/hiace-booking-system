package com.hiace.hiacebooking.service;

import java.util.List;

import com.hiace.hiacebooking.model.Booking;

public interface BookingService {
	
	Booking createBooking(Long routeId, int seatNumber, String username);
	int autoAssignSeat(Long routeId);
	List<Booking> getBookingByUsername(String username);
	List<Booking> getAllBookings();
	Booking getBookingById(Long id);
	void confirmPayment(String transactionId);
	void cancelBooking(Long bookingId);
	List<Booking> getBookingsSortedByDate(String username);
	double calculateFarePriority(Long routeId);

}
