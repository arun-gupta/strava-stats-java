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

    public List<HeatmapDataDto> getWorkoutHeatmapData(List<StravaActivity> activities) {
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

    public WorkoutHeatmapDto getWorkoutHeatmapSummary(List<StravaActivity> activities, LocalDate referenceDate) {
        if (referenceDate == null) referenceDate = LocalDate.now();

        // Build a sorted unique set of dates with any activity
        final NavigableSet<LocalDate> activityDates = activities.stream()
                .map(a -> a.getStartDateLocal().toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));

        if (activityDates.isEmpty()) {
            return WorkoutHeatmapDto.builder()
                    .currentStreak(0)
                    .longestStreak(0)
                    .workoutDays(0)
                    .missedDays(0)
                    .daysSinceLast(referenceDate != null ? 0 : 0)
                    .longestGap(0)
                    .totalGapDays(0)
                    .rangeStart(referenceDate)
                    .rangeEnd(referenceDate)
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

        // Determine range for metrics/timeline
        LocalDate rangeStart = activityDates.first();
        LocalDate rangeEnd = referenceDate;
        if (rangeEnd.isBefore(rangeStart)) {
            // fallback to at least cover the reference day
            rangeStart = rangeEnd;
        }

        // Count workout days within the range
        int workoutDays = 0;
        for (LocalDate d : activityDates) {
            if (!d.isBefore(rangeStart) && !d.isAfter(rangeEnd)) {
                workoutDays++;
            }
        }

        int totalDays = (int) (ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1);

        // Days since last activity up to rangeEnd
        NavigableSet<LocalDate> uptoEnd = activityDates.headSet(rangeEnd, true);
        LocalDate lastActivityOnOrBeforeEnd = uptoEnd.isEmpty()
                ? null
                : uptoEnd.last();
        int daysSinceLast = lastActivityOnOrBeforeEnd == null ? totalDays
                : (int) ChronoUnit.DAYS.between(lastActivityOnOrBeforeEnd, rangeEnd);

        // Missed days over the full requested range (for consistency with displayed range).
        int missedDays = Math.max(totalDays - workoutDays, 0);

        // Compute gaps inside the range.
        // Important: do not count the trailing "open" gap after the last activity day.
        // Strava's gaps typically refer to completed no-activity periods between workouts,
        // not the days since the most recent workout up to today.
        int longestGap = 0;
        int totalGapDays = 0;
        int currentGap = 0;
        LocalDate gapEndInclusive = lastActivityOnOrBeforeEnd != null ? lastActivityOnOrBeforeEnd : rangeEnd;
        for (LocalDate day = rangeStart; !day.isAfter(gapEndInclusive); day = day.plusDays(1)) {
            if (!activityDates.contains(day)) {
                currentGap++;
                totalGapDays++;
                if (currentGap > longestGap) longestGap = currentGap;
            } else {
                currentGap = 0;
            }
        }

        return WorkoutHeatmapDto.builder()
                .currentStreak(current)
                .longestStreak(longest)
                .longestStreakStart(longestStart)
                .longestStreakEnd(longestEnd)
                .workoutDays(workoutDays)
                .missedDays(missedDays)
                .daysSinceLast(daysSinceLast)
                .longestGap(longestGap)
                .totalGapDays(totalGapDays)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
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

    public List<RunDistributionDto> getRunDistribution(List<StravaActivity> activities) {
        List<StravaActivity> runs = activities.stream()
                .filter(a -> "Run".equalsIgnoreCase(a.getType()) ||
                           (a.getSportType() != null && a.getSportType().toLowerCase().contains("run")))
                .collect(Collectors.toList());

        // Define distance ranges in miles
        String[] ranges = {"0-1", "1-2", "2-3", "3-4", "4-5", "5-6", "6-7", "7-8", "8-9", "9-10"};
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (String range : ranges) {
            distribution.put(range, 0L);
        }

        // Count runs in each range
        for (StravaActivity run : runs) {
            if (run.getDistance() != null) {
                double miles = run.getDistance() * METERS_TO_MILES;
                int bucket = (int) miles; // floor to get the range
                if (bucket >= 0 && bucket < 10) {
                    String range = bucket + "-" + (bucket + 1);
                    distribution.put(range, distribution.getOrDefault(range, 0L) + 1);
                }
            }
        }

        return distribution.entrySet().stream()
                .map(entry -> new RunDistributionDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
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
