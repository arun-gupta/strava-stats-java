package com.example.strava.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RunStatsDto {
    private int totalRuns;
    private int runs10KPlus;
    private double totalMiles;
    private String averagePace; // MM:SS format
    private String fastestMileSplit;
    private String fastest10K;
    private double longestRun; // in miles
    private double mostElevation; // in feet
}
