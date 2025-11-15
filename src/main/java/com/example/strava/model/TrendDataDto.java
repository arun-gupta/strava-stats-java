package com.example.strava.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrendDataDto {
    private String label; // date, week, or month label
    private double value; // miles or pace
    private String formattedValue; // formatted display value
}
