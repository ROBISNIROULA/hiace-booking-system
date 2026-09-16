package com.hiace.hiacebooking.serviceimpl;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hiace.hiacebooking.constants.BookingStatus;
import com.hiace.hiacebooking.model.Booking;
import com.hiace.hiacebooking.model.Route;
import com.hiace.hiacebooking.repository.BookingRepository;
import com.hiace.hiacebooking.repository.RouteRepository;
import com.hiace.hiacebooking.service.RouteService;

@Service
public class RouteServiceImpl implements RouteService{

	@Autowired
	private RouteRepository routeRepository;
	@Autowired
	private BookingRepository bookingRepository;
	
	@Override
	public List<Route> getAllRoutes() {
		
		return routeRepository.findAll();
	}

	@Override
	public List<Route> getRoutesSortedByFare() {
		List<Route> routes = getActiveRoutes();
		quickSort(routes, 0, routes.size() - 1);
		return routes;
	}
	 private void quickSort(List<Route> routes, int low, int high) {
	        if (low < high) {
	            int pi = partition(routes, low, high);
	            quickSort(routes, low, pi - 1);
	            quickSort(routes, pi + 1, high);
	        }
	    }

	    private int partition(List<Route> routes, int low, int high) {
	        double pivot = routes.get(high).getFare();
	        int i = low - 1;
	        for (int j = low; j < high; j++) {
	            if (routes.get(j).getFare() <= pivot) {
	                i++;
	                Route temp = routes.get(i);
	                routes.set(i, routes.get(j));
	                routes.set(j, temp);
	            }
	        }
	        Route temp = routes.get(i + 1);
	        routes.set(i + 1, routes.get(high));
	        routes.set(high, temp);
	        return i + 1;
	    }

	@Override
	public Route searchByDestination(String destination) {
        List<Route> routes = getRoutesSortedByFare();
        
        routes.sort((a, b) -> a.getDestination()
                               .compareToIgnoreCase(b.getDestination()));
        int low = 0, high = routes.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int cmp = routes.get(mid).getDestination()
                           .compareToIgnoreCase(destination);
            if (cmp == 0) return routes.get(mid);
            else if (cmp < 0) low = mid + 1;
            else high = mid - 1;
        }
        
		return null;
	}

	@Override
	public Route getRouteById(Long id) {
		
		return routeRepository.findById(id).orElse(null);
	}

	@Override
	public void saveRoute(Route route) {
		routeRepository.save(route);
		
	}

	@Override
	public void deleteRoute(Long id) {
		routeRepository.deleteById(id);
		
	}

	@Override
	public List<Route> getRoutesSortedByDate() {
	    List<Route> routes = getActiveRoutes();

	    mergeSortByDate(routes, 0, routes.size() - 1);
	    return routes;
	}

	private void mergeSortByDate(List<Route> list, int left, int right) {
	    if (left < right) {
	        int mid = (left + right) / 2;
	        mergeSortByDate(list, left, mid);
	        mergeSortByDate(list, mid + 1, right);
	        mergeByDate(list, left, mid, right);
	    }
	}

	private void mergeByDate(List<Route> list,
	                          int left, int mid, int right) {
	    List<Route> leftList  =
	        new ArrayList<>(list.subList(left, mid + 1));
	    List<Route> rightList =
	        new ArrayList<>(list.subList(mid + 1, right + 1));

	    int i = 0, j = 0, k = left;
	    while (i < leftList.size() && j < rightList.size()) {
	        // Compare dates — nearest date first
	        if (!leftList.get(i).getDepartureDate()
	                .isAfter(rightList.get(j).getDepartureDate())) {
	            list.set(k++, leftList.get(i++));
	        } else {
	            list.set(k++, rightList.get(j++));
	        }
	    }
	    while (i < leftList.size())  list.set(k++, leftList.get(i++));
	    while (j < rightList.size()) list.set(k++, rightList.get(j++));
	}

	@Override
	public List<Route> searchRoutes(String destination) {
	    if (destination == null || destination.trim().isEmpty()) {
	        return getRoutesSortedByDate();
	    }
	    return getActiveRoutes()   // ← only future/today routes
	            .stream()
	            .filter(r -> r.getDestination()
	                          .toLowerCase()
	                          .contains(destination.toLowerCase()) ||
	                         r.getSource()
	                          .toLowerCase()
	                          .contains(destination.toLowerCase()))
	            .sorted((a, b) -> a.getDepartureDate()
	                               .compareTo(b.getDepartureDate()))
	            .collect(java.util.stream.Collectors.toList());
	}
	
	// ── Returns only routes whose departure date is today or future ──
	@Override
	public List<Route> getActiveRoutes() {
	    return routeRepository.findAll()
	            .stream()
	            .filter(r -> r.getDepartureDate() != null &&
	                         !r.getDepartureDate()
	                           .isBefore(java.time.LocalDate.now()))
	            .collect(java.util.stream.Collectors.toList());
	}
	@Override
	public List<Route> getRoutesByPriority() {

	    // Count confirmed bookings for each route
	    Map<Route, Integer> bookingCount = new HashMap<>();

	    for (Booking booking : bookingRepository.findAll()) {

	        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {

	            Route route = booking.getRoute();

	            bookingCount.put(route,
	                    bookingCount.getOrDefault(route, 0) + 1);
	        }
	    }

	    // Max Priority Queue
	    PriorityQueue<Route> priorityQueue =
	            new PriorityQueue<>(
	                    (r1, r2) -> Integer.compare(
	                            bookingCount.getOrDefault(r2, 0),
	                            bookingCount.getOrDefault(r1, 0))
	            );

	    priorityQueue.addAll(bookingCount.keySet());

	    List<Route> sortedRoutes = new ArrayList<>();

	    while (!priorityQueue.isEmpty()) {
	        sortedRoutes.add(priorityQueue.poll());
	    }

	    return sortedRoutes;
	}

}
