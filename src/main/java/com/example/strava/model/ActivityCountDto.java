package com.example.strava.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActivityCountDto {
    private String activityType;
    private long count;
    private double percentage;
}
