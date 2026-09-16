package com.hiace.hiacebooking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiace.hiacebooking.model.Hiace;
import com.hiace.hiacebooking.model.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long>{

	List<Seat> findByHiace(Hiace hiace);
	List<Seat> findByHiaceAndIsBooked(Hiace hiace, boolean isBooked);
	
}
