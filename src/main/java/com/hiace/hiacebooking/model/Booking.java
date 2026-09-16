package com.hiace.hiacebooking.model;

import java.time.LocalDateTime;

import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.constants.PaymentStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bookingId;
	
	private int seatNumber;
	private LocalDateTime bookingDate;
	
	@Enumerated(EnumType.STRING)
	private BookingStatus bookingStatus;
	
	@Enumerated(EnumType.STRING)
	private PaymentStatus paymentStatus;
	
	private String transactionId;
	
	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;
	
	@ManyToOne
	@JoinColumn(name = "route_id")
	private Route route;
	

}
