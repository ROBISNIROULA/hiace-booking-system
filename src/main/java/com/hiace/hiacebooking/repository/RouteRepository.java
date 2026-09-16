package com.hiace.hiacebooking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hiace.hiacebooking.model.Hiace;
import com.hiace.hiacebooking.model.Route;

public interface RouteRepository extends JpaRepository<Route, Long>{

	List<Route> findBySourceAndDestination(String source, String destination);
	List<Route> findByDestinationContainingIgnoreCase(String destination);
	List<Route> findByHiace(Hiace hiace);
}
