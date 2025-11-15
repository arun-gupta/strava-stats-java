package com.example.strava.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeDistributionDto {
    private String activityType;
    private double hours;
    private String formattedTime; // HH:MM format
    private double percentage;
}
