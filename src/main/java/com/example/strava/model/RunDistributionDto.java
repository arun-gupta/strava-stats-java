package com.example.strava.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RunDistributionDto {
    private String distanceRange; // e.g., "0-1", "1-2", "5-6"
    private long count; // number of runs in this range
}
