package com.hiace.hiacebooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteGroupDto {

    private String source;
    private String destination;
    private double minFare;
    private LocalDate nextDate;     // nearest upcoming departure date for this pair
    private int hiaceCount;         // number of distinct hiaces serving this pair
    private List<String> categories; // distinct categories among those hiaces (e.g. DELUXE, NORMAL)
    private String imageName;       // image of one representative hiace, used as card background
}