package com.example.strava.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class HeatmapDataDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private double value; // hours or miles depending on usage
    private int intensity; // 0-4 scale for color coding
}
