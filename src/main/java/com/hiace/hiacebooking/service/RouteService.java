package com.hiace.hiacebooking.service;

import java.util.List;

import com.hiace.hiacebooking.model.Route;

public interface RouteService {
	
	List<Route> getAllRoutes();
	List<Route> getRoutesSortedByFare();
    List<Route> getRoutesSortedByDate();        
    List<Route> searchRoutes(String destination);
    List<Route> getActiveRoutes();
    List<Route> getRoutesByPriority();
	Route searchByDestination(String dest);
	Route getRouteById(Long id);
	void saveRoute(Route route);
	void deleteRoute(Long id);

}
