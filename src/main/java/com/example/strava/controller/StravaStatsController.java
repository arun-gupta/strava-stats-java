package com.example.strava.controller;

import com.example.strava.dto.SummaryStatsDto;
import com.example.strava.model.*;
import com.example.strava.service.StravaApiService;
import com.example.strava.service.StravaStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StravaStatsController {

    private final StravaApiService stravaApiService;
    private final StravaStatsService stravaStatsService;

    public StravaStatsController(StravaApiService stravaApiService, StravaStatsService stravaStatsService) {
        this.stravaApiService = stravaApiService;
        this.stravaStatsService = stravaStatsService;
    }

    private void validateDateRange(LocalDate after, LocalDate before) {
        if (after != null && before != null && after.isAfter(before)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    @GetMapping("/summary")
    public SummaryStatsDto getSummary(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getSummaryStats(activities);
    }

    @GetMapping("/activity-count")
    public List<ActivityCountDto> getActivityCountDistribution(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getActivityCountDistribution(activities);
    }

    @GetMapping("/time-distribution")
    public List<TimeDistributionDto> getTimeDistribution(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getTimeDistribution(activities);
    }

    @GetMapping("/workout-heatmap")
    public List<HeatmapDataDto> getWorkoutHeatmap(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getWorkoutHeatmapData(activities);
    }

    @GetMapping("/workout-heatmap/summary")
    public WorkoutHeatmapDto getWorkoutHeatmapSummary(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        LocalDate reference = (before != null) ? before : LocalDate.now();
        return stravaStatsService.getWorkoutHeatmapSummary(activities, reference, after);
    }

    @GetMapping("/run-statistics")
    public RunStatsDto getRunStatistics(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getRunStatistics(activities);
    }

    @GetMapping("/run-distribution")
    public List<RunDistributionDto> getRunDistribution(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getRunDistribution(activities);
    }

    @GetMapping("/running-heatmap")
    public List<HeatmapDataDto> getRunningHeatmap(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getRunningHeatmap(activities);
    }

    @GetMapping("/mileage-trend")
    public List<TrendDataDto> getMileageTrend(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getMileageTrend(activities, period);
    }

    @GetMapping("/pace-trend")
    public List<TrendDataDto> getPaceTrend(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {

        validateDateRange(after, before);
        List<StravaActivity> activities = stravaApiService.getAllActivities(principal.getName(), after, before);
        return stravaStatsService.getPaceTrend(activities, period);
    }
}
