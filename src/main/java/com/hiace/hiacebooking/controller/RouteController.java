package com.hiace.hiacebooking.controller;

import com.hiace.hiacebooking.dto.RouteGroupDto;
import com.hiace.hiacebooking.model.Route;
import com.hiace.hiacebooking.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    // ── EXISTING — unchanged ──────────────────────────────────────────────────
    @GetMapping("/list")
    public String viewRoutes(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            Model model) {

        List<Route> routes;

        if (source != null && !source.isEmpty()
                && destination != null && !destination.isEmpty()) {
            routes = routeService.getAllRoutes()
                    .stream()
                    .filter(r ->
                        r.getSource().toLowerCase()
                         .contains(source.toLowerCase()) &&
                        r.getDestination().toLowerCase()
                         .contains(destination.toLowerCase()))
                    .toList();
        } else if (source != null && !source.isEmpty()) {
            routes = routeService.getAllRoutes()
                    .stream()
                    .filter(r ->
                        r.getSource().toLowerCase()
                         .contains(source.toLowerCase()))
                    .toList();
        } else {
            routes = routeService.getAllRoutes();
        }

        model.addAttribute("routes", routes);
        model.addAttribute("source", source);
        model.addAttribute("destination", destination);

        return "route-list";
    }

    // ── NEW — grouped overview page for navbar "Routes" link ───────────────────
    // Maps to GET /routes  (root of this controller's mapping)
    @GetMapping
    public String viewGroupedRoutes(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            Model model) {

        List<Route> activeRoutes = routeService.getActiveRoutes();

        if (source != null && !source.trim().isEmpty()) {
            String s = source.trim().toLowerCase();
            activeRoutes = activeRoutes.stream()
                    .filter(r -> r.getSource().toLowerCase().contains(s))
                    .collect(Collectors.toList());
        }

        if (destination != null && !destination.trim().isEmpty()) {
            String d = destination.trim().toLowerCase();
            activeRoutes = activeRoutes.stream()
                    .filter(r -> r.getDestination().toLowerCase().contains(d))
                    .collect(Collectors.toList());
        }

        Map<String, List<Route>> grouped = activeRoutes.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSource() + "->" + r.getDestination(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<RouteGroupDto> routeGroups = new ArrayList<>();

        for (List<Route> group : grouped.values()) {
            Route first = group.get(0);

            double minFare = group.stream()
                    .mapToDouble(Route::getFare)
                    .min()
                    .orElse(0);

            LocalDate nextDate = group.stream()
                    .map(Route::getDepartureDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            Set<String> distinctHiaceIds = group.stream()
                    .map(r -> r.getHiace().getHiaceId().toString())
                    .collect(Collectors.toSet());

            List<String> categories = group.stream()
                    .map(r -> r.getHiace().getCategory().name())
                    .distinct()
                    .collect(Collectors.toList());

            String imageName = group.stream()
                    .map(r -> r.getHiace().getImageName())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            routeGroups.add(RouteGroupDto.builder()
                    .source(first.getSource())
                    .destination(first.getDestination())
                    .minFare(minFare)
                    .nextDate(nextDate)
                    .hiaceCount(distinctHiaceIds.size())
                    .categories(categories)
                    .imageName(imageName)
                    .build());
        }

        routeGroups.sort((a, b) -> {
            if (a.getNextDate() == null && b.getNextDate() == null) return 0;
            if (a.getNextDate() == null) return 1;
            if (b.getNextDate() == null) return -1;
            return a.getNextDate().compareTo(b.getNextDate());
        });

        model.addAttribute("routeGroups", routeGroups);
        model.addAttribute("selectedSource", source);
        model.addAttribute("selectedDestination", destination);

        return "routes";
    }
}