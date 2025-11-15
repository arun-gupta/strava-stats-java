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
public class WorkoutStreakDto {
    private int currentStreak; // consecutive days ending at reference date
    private int longestStreak;
    private LocalDate longestStreakStart;
    private LocalDate longestStreakEnd;
}
