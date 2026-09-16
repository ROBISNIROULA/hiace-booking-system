package com.hiace.hiacebooking.model;

import com.hiace.hiacebooking.constants.SeatCategory;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hiaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hiace {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long hiaceId;
	
	private String vehicleNumber;
	private String driverName;
	private int totalSeats;
	
	@Enumerated(EnumType.STRING)
	private SeatCategory category;
	
	private String imageName;

}
