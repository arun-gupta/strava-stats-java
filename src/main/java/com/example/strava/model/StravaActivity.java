package com.example.strava.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class StravaActivity {
    private Long id;
    private String name;

    @JsonProperty("sport_type")
    private String sportType;

    private String type;

    private Double distance; // in meters

    @JsonProperty("moving_time")
    private Integer movingTime; // in seconds

    @JsonProperty("elapsed_time")
    private Integer elapsedTime; // in seconds

    @JsonProperty("total_elevation_gain")
    private Double totalElevationGain; // in meters

    @JsonProperty("start_date")
    private ZonedDateTime startDate;

    @JsonProperty("start_date_local")
    private ZonedDateTime startDateLocal;

    @JsonProperty("average_speed")
    private Double averageSpeed; // in meters per second

    @JsonProperty("max_speed")
    private Double maxSpeed; // in meters per second
}
