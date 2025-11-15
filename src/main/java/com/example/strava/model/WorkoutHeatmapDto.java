package com.example.strava.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutHeatmapDto {
    private int currentStreak; // consecutive days ending at reference date
    private int longestStreak;
    private LocalDate longestStreakStart;
    private LocalDate longestStreakEnd;
    // Additional consistency metrics
    private int workoutDays;      // count of days with at least one activity in range
    private int missedDays;       // count of days with no activity in range
    private int daysSinceLast;    // days since most recent activity up to reference end date
    private int longestGap;       // longest consecutive no-activity gap (days)
    private int totalGapDays;     // sum of all gap days in range
    private LocalDate rangeStart; // start date used for timeline/metrics
    private LocalDate rangeEnd;   // end/reference date used for timeline/metrics
}
