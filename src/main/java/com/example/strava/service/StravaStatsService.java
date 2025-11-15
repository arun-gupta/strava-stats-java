package com.example.strava.service;

import com.example.strava.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StravaStatsService {

    private static final double METERS_TO_MILES = 0.000621371;
    private static final double METERS_TO_FEET = 3.28084;
    private static final double METERS_PER_SECOND_TO_MILES_PER_HOUR = 2.23694;

    public List<ActivityCountDto> getActivityCountDistribution(List<StravaActivity> activities) {
        long total = activities.size();
        if (total == 0) return Collections.emptyList();

        Map<String, Long> counts = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getSportType() != null ? a.getSportType() : a.getType(),
                        Collectors.counting()
                ));

        return counts.entrySet().stream()
                .map(entry -> new ActivityCountDto(
                        entry.getKey(),
                        entry.getValue(),
                        (entry.getValue() * 100.0) / total
                ))
                .sorted(Comparator.comparing(ActivityCountDto::getCount).reversed())
                .collect(Collectors.toList());
    }

    public List<TimeDistributionDto> getTimeDistribution(List<StravaActivity> activities) {
        Map<String, Integer> timeByType = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getSportType() != null ? a.getSportType() : a.getType(),
                        Collectors.summingInt(a -> a.getMovingTime() != null ? a.getMovingTime() : 0)
                ));

        int totalSeconds = timeByType.values().stream().mapToInt(Integer::intValue).sum();
        if (totalSeconds == 0) return Collections.emptyList();

        return timeByType.entrySet().stream()
                .map(entry -> {
                    double hours = entry.getValue() / 3600.0;
                    String formatted = formatTime(entry.getValue());
                    double percentage = (entry.getValue() * 100.0) / totalSeconds;
                    return new TimeDistributionDto(entry.getKey(), hours, formatted, percentage);
                })
                .sorted(Comparator.comparing(TimeDistributionDto::getHours).reversed())
                .collect(Collectors.toList());
    }

    public List<HeatmapDataDto> getWorkoutStreaksHeatmap(List<StravaActivity> activities) {
        Map<LocalDate, Double> dailyHours = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartDateLocal().toLocalDate(),
                        Collectors.summingDouble(a -> (a.getMovingTime() != null ? a.getMovingTime() : 0) / 3600.0)
                ));

        return dailyHours.entrySet().stream()
                .map(entry -> {
                    double hours = entry.getValue();
                    int intensity = calculateIntensity(hours, 0, 1, 2, 3); // 0, 1-2, 2-3, 3+ hours
                    return new HeatmapDataDto(entry.getKey(), hours, intensity);
                })
                .sorted(Comparator.comparing(HeatmapDataDto::getDate))
                .collect(Collectors.toList());
    }

    public WorkoutStreakDto getWorkoutStreakSummary(List<StravaActivity> activities, LocalDate referenceDate) {
        if (referenceDate == null) referenceDate = LocalDate.now();

        // Build a sorted unique set of dates with any activity
        SortedSet<LocalDate> activityDates = activities.stream()
                .map(a -> a.getStartDateLocal().toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));

        if (activityDates.isEmpty()) {
            return WorkoutStreakDto.builder()
                    .currentStreak(0)
                    .longestStreak(0)
                    .build();
        }

        // Current streak: count back from referenceDate
        int current = 0;
        LocalDate cursor = referenceDate;
        while (activityDates.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }

        // Longest streak over all dates
        int longest = 0;
        LocalDate longestStart = null;
        LocalDate longestEnd = null;

        LocalDate streakStart = null;
        LocalDate prev = null;
        for (LocalDate d : activityDates) {
            if (prev == null || !d.equals(prev.plusDays(1))) {
                // new streak
                streakStart = d;
            }
            int length = (int) (ChronoUnit.DAYS.between(streakStart, d) + 1);
            if (length > longest) {
                longest = length;
                longestStart = streakStart;
                longestEnd = d;
            }
            prev = d;
        }

        return WorkoutStreakDto.builder()
                .currentStreak(current)
                .longestStreak(longest)
                .longestStreakStart(longestStart)
                .longestStreakEnd(longestEnd)
                .build();
    }

    public RunStatsDto getRunStatistics(List<StravaActivity> activities) {
        List<StravaActivity> runs = activities.stream()
                .filter(a -> "Run".equalsIgnoreCase(a.getType()) ||
                           (a.getSportType() != null && a.getSportType().toLowerCase().contains("run")))
                .collect(Collectors.toList());

        if (runs.isEmpty()) {
            return RunStatsDto.builder()
                    .totalRuns(0)
                    .runs10KPlus(0)
                    .totalMiles(0)
                    .averagePace("00:00")
                    .fastestMileSplit("00:00")
                    .fastest10K("00:00")
                    .longestRun(0)
                    .mostElevation(0)
                    .build();
        }

        int totalRuns = runs.size();
        int runs10KPlus = (int) runs.stream()
                .filter(r -> r.getDistance() != null && r.getDistance() >= 10000)
                .count();

        double totalMiles = runs.stream()
                .mapToDouble(r -> (r.getDistance() != null ? r.getDistance() : 0) * METERS_TO_MILES)
                .sum();

        double totalSeconds = runs.stream()
                .mapToDouble(r -> r.getMovingTime() != null ? r.getMovingTime() : 0)
                .sum();
        String averagePace = calculatePace(totalMiles, totalSeconds);

        String fastestMileSplit = runs.stream()
                .filter(r -> r.getDistance() != null && r.getDistance() >= 1609.34) // at least 1 mile
                .map(r -> calculatePace(r.getDistance() * METERS_TO_MILES, r.getMovingTime()))
                .min(String::compareTo)
                .orElse("00:00");

        String fastest10K = runs.stream()
                .filter(r -> r.getDistance() != null && r.getDistance() >= 10000)
                .map(r -> formatTime(r.getMovingTime()))
                .min(String::compareTo)
                .orElse("00:00");

        double longestRun = runs.stream()
                .mapToDouble(r -> (r.getDistance() != null ? r.getDistance() : 0) * METERS_TO_MILES)
                .max()
                .orElse(0);

        double mostElevation = runs.stream()
                .mapToDouble(r -> (r.getTotalElevationGain() != null ? r.getTotalElevationGain() : 0) * METERS_TO_FEET)
                .max()
                .orElse(0);

        return RunStatsDto.builder()
                .totalRuns(totalRuns)
                .runs10KPlus(runs10KPlus)
                .totalMiles(Math.round(totalMiles * 100.0) / 100.0)
                .averagePace(averagePace)
                .fastestMileSplit(fastestMileSplit)
                .fastest10K(fastest10K)
                .longestRun(Math.round(longestRun * 100.0) / 100.0)
                .mostElevation(Math.round(mostElevation))
                .build();
    }

    public List<HeatmapDataDto> getRunningHeatmap(List<StravaActivity> activities) {
        List<StravaActivity> runs = activities.stream()
                .filter(a -> "Run".equalsIgnoreCase(a.getType()) ||
                           (a.getSportType() != null && a.getSportType().toLowerCase().contains("run")))
                .collect(Collectors.toList());

        Map<LocalDate, Double> dailyMiles = runs.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartDateLocal().toLocalDate(),
                        Collectors.summingDouble(a -> (a.getDistance() != null ? a.getDistance() : 0) * METERS_TO_MILES)
                ));

        return dailyMiles.entrySet().stream()
                .map(entry -> {
                    double miles = entry.getValue();
                    int intensity = calculateIntensity(miles, 0, 3, 6, 10); // 0, 1-3, 3-6, 6-10, 10+ miles
                    return new HeatmapDataDto(entry.getKey(), miles, intensity);
                })
                .sorted(Comparator.comparing(HeatmapDataDto::getDate))
                .collect(Collectors.toList());
    }

    public List<TrendDataDto> getMileageTrend(List<StravaActivity> activities, String period) {
        List<StravaActivity> runs = activities.stream()
                .filter(a -> "Run".equalsIgnoreCase(a.getType()) ||
                           (a.getSportType() != null && a.getSportType().toLowerCase().contains("run")))
                .collect(Collectors.toList());

        switch (period.toLowerCase()) {
            case "daily":
                return getDailyMileageTrend(runs);
            case "weekly":
                return getWeeklyMileageTrend(runs);
            case "monthly":
                return getMonthlyMileageTrend(runs);
            default:
                return Collections.emptyList();
        }
    }

    public List<TrendDataDto> getPaceTrend(List<StravaActivity> activities, String period) {
        List<StravaActivity> runs = activities.stream()
                .filter(a -> "Run".equalsIgnoreCase(a.getType()) ||
                           (a.getSportType() != null && a.getSportType().toLowerCase().contains("run")))
                .collect(Collectors.toList());

        switch (period.toLowerCase()) {
            case "daily":
                return getDailyPaceTrend(runs);
            case "weekly":
                return getWeeklyPaceTrend(runs);
            case "monthly":
                return getMonthlyPaceTrend(runs);
            default:
                return Collections.emptyList();
        }
    }

    // Helper methods
    private String formatTime(Integer seconds) {
        if (seconds == null || seconds == 0) return "00:00";
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private String calculatePace(double miles, double seconds) {
        if (miles == 0) return "00:00";
        double paceSeconds = seconds / miles;
        int minutes = (int) (paceSeconds / 60);
        int secs = (int) (paceSeconds % 60);
        return String.format("%02d:%02d", minutes, secs);
    }

    private int calculateIntensity(double value, double... thresholds) {
        for (int i = 0; i < thresholds.length; i++) {
            if (value <= thresholds[i]) return i;
        }
        return thresholds.length;
    }

    private List<TrendDataDto> getDailyMileageTrend(List<StravaActivity> runs) {
        Map<LocalDate, Double> dailyMiles = runs.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartDateLocal().toLocalDate(),
                        Collectors.summingDouble(a -> (a.getDistance() != null ? a.getDistance() : 0) * METERS_TO_MILES)
                ));

        return dailyMiles.entrySet().stream()
                .map(entry -> new TrendDataDto(
                        entry.getKey().toString(),
                        entry.getValue(),
                        String.format("%.2f mi", entry.getValue())
                ))
                .sorted(Comparator.comparing(TrendDataDto::getLabel))
                .collect(Collectors.toList());
    }

    private List<TrendDataDto> getWeeklyMileageTrend(List<StravaActivity> runs) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<String, Double> weeklyMiles = runs.stream()
                .collect(Collectors.groupingBy(
                        a -> {
                            LocalDate date = a.getStartDateLocal().toLocalDate();
                            int weekNum = date.get(weekFields.weekOfWeekBasedYear());
                            int year = date.get(weekFields.weekBasedYear());
                            return year + "-W" + String.format("%02d", weekNum);
                        },
                        Collectors.summingDouble(a -> (a.getDistance() != null ? a.getDistance() : 0) * METERS_TO_MILES)
                ));

        return weeklyMiles.entrySet().stream()
                .map(entry -> new TrendDataDto(
                        entry.getKey(),
                        entry.getValue(),
                        String.format("%.2f mi", entry.getValue())
                ))
                .sorted(Comparator.comparing(TrendDataDto::getLabel))
                .collect(Collectors.toList());
    }

    private List<TrendDataDto> getMonthlyMileageTrend(List<StravaActivity> runs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Double> monthlyMiles = runs.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartDateLocal().toLocalDate().format(formatter),
                        Collectors.summingDouble(a -> (a.getDistance() != null ? a.getDistance() : 0) * METERS_TO_MILES)
                ));

        return monthlyMiles.entrySet().stream()
                .map(entry -> new TrendDataDto(
                        entry.getKey(),
                        entry.getValue(),
                        String.format("%.2f mi", entry.getValue())
                ))
                .sorted(Comparator.comparing(TrendDataDto::getLabel))
                .collect(Collectors.toList());
    }

    private List<TrendDataDto> getDailyPaceTrend(List<StravaActivity> runs) {
        Map<LocalDate, double[]> dailyData = runs.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartDateLocal().toLocalDate(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    double totalMiles = list.stream().mapToDouble(a -> (a.getDistance() != null ? a.getDistance() : 0) * METERS_TO_MILES).sum();
                                    double totalSeconds = list.stream().mapToDouble(a -> a.getMovingTime() != null ? a.getMovingTime() : 0).sum();
                                    return new double[]{totalMiles, totalSeconds};
                                }
                        )
                ));

        return dailyData.entrySet().stream()
                .map(entry -> {
                    String pace = calculatePace(entry.getValue()[0], entry.getValue()[1]);
                    double paceValue = parsePaceToSeconds(pace);
                    return new TrendDataDto(entry.getKey().toString(), paceValue, pace + " /mi");
                })
                .sorted(Comparator.comparing(TrendDataDto::getLabel))
                .collect(Collectors.toList());
    }

    private List<TrendDataDto> getWeeklyPaceTrend(List<StravaActivity> runs) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<String, double[]> weeklyData = runs.stream()
                .collect(Collectors.groupingBy(
                        a -> {
                            LocalDate date = a.getStartDateLocal().toLocalDate();
                            int weekNum = date.get(weekFields.weekOfWeekBasedYear());
                            int year = date.get(weekFields.weekBasedYear());
                            return year + "-W" + String.format("%02d", weekNum);
                        },
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    double totalMiles = list.stream().mapToDouble(a -> (a.getDistance() != null ? a.getDistance() : 0) * METERS_TO_MILES).sum();
                                    double totalSeconds = list.stream().mapToDouble(a -> a.getMovingTime() != null ? a.getMovingTime() : 0).sum();
                                    return new double[]{totalMiles, totalSeconds};
                                }
                        )
                ));

        return weeklyData.entrySet().stream()
                .map(entry -> {
                    String pace = calculatePace(entry.getValue()[0], entry.getValue()[1]);
                    double paceValue = parsePaceToSeconds(pace);
                    return new TrendDataDto(entry.getKey(), paceValue, pace + " /mi");
                })
                .sorted(Comparator.comparing(TrendDataDto::getLabel))
                .collect(Collectors.toList());
    }

    private List<TrendDataDto> getMonthlyPaceTrend(List<StravaActivity> runs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, double[]> monthlyData = runs.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartDateLocal().toLocalDate().format(formatter),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    double totalMiles = list.stream().mapToDouble(a -> (a.getDistance() != null ? a.getDistance() : 0) * METERS_TO_MILES).sum();
                                    double totalSeconds = list.stream().mapToDouble(a -> a.getMovingTime() != null ? a.getMovingTime() : 0).sum();
                                    return new double[]{totalMiles, totalSeconds};
                                }
                        )
                ));

        return monthlyData.entrySet().stream()
                .map(entry -> {
                    String pace = calculatePace(entry.getValue()[0], entry.getValue()[1]);
                    double paceValue = parsePaceToSeconds(pace);
                    return new TrendDataDto(entry.getKey(), paceValue, pace + " /mi");
                })
                .sorted(Comparator.comparing(TrendDataDto::getLabel))
                .collect(Collectors.toList());
    }

    private double parsePaceToSeconds(String pace) {
        String[] parts = pace.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
