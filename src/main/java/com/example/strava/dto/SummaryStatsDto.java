package com.example.strava.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SummaryStatsDto {
    private int totalActivities;
    private int totalMovingTimeSeconds;
}
