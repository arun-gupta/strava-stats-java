package com.example.strava.service;

import com.example.strava.model.StravaActivity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class StravaApiService {

    private static final Logger logger = LoggerFactory.getLogger(StravaApiService.class);

    private final WebClient webClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public StravaApiService(
            @Value("${strava.api.base-url}") String baseUrl,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.authorizedClientService = authorizedClientService;
    }

    @Retry(name = "stravaApi", fallbackMethod = "getActivitiesFallback")
    @CircuitBreaker(name = "stravaApi", fallbackMethod = "getActivitiesFallback")
    public List<StravaActivity> getActivities(String principalName, LocalDate after, LocalDate before, int perPage) {
        logger.debug("Fetching activities for user: {}", principalName);

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("strava", principalName);

        if (client == null) {
            throw new IllegalStateException("No authorized client found for: " + principalName);
        }

        OAuth2AccessToken accessToken = client.getAccessToken();

        // Use UTC for API parameters, but we'll filter based on startDateLocal after fetching
        // Add buffer of 1 day on each side to account for timezone differences
        ZoneId utc = ZoneId.of("UTC");
        long afterEpoch = after != null ? after.minusDays(1).atStartOfDay(utc).toEpochSecond() : 0;
        long beforeEpoch = before != null
            ? before.plusDays(1).atTime(LocalTime.MAX).atZone(utc).toEpochSecond() + 1
            : System.currentTimeMillis() / 1000;

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/athlete/activities")
                        .queryParam("after", afterEpoch)
                        .queryParam("before", beforeEpoch)
                        .queryParam("per_page", perPage)
                        .build())
                .header("Authorization", "Bearer " + accessToken.getTokenValue())
                .retrieve()
                .bodyToFlux(StravaActivity.class)
                .collectList()
                .block();
    }

    private List<StravaActivity> getActivitiesFallback(String principalName, LocalDate after, LocalDate before, int perPage, Exception ex) {
        logger.error("Fallback triggered for getActivities due to: {}", ex.getMessage());
        return Collections.emptyList();
    }

    public List<StravaActivity> getAllActivities(String principalName, LocalDate after, LocalDate before) {
        // Strava API returns activities in reverse chronological order (newest first)
        // It only returns up to 200 activities per call. To get all activities, we need to
        // make multiple calls with adjusted date ranges, using the oldest activity's date
        // as the new 'before' date for the next call.
        List<StravaActivity> allActivities = new ArrayList<>();
        int perPage = 200; // Strava's max per page
        LocalDate currentBefore = before;
        int maxIterations = 50; // Safety limit to prevent infinite loops
        int iteration = 0;

        while (iteration < maxIterations) {
            List<StravaActivity> pageActivities = getActivities(principalName, after, currentBefore, perPage);

            if (pageActivities == null || pageActivities.isEmpty()) {
                break; // No more activities
            }

            allActivities.addAll(pageActivities);

            // If we got fewer than perPage activities, we've reached the end
            if (pageActivities.size() < perPage) {
                break;
            }

            // Get the oldest activity from this page (last in the list since it's reverse chronological)
            StravaActivity oldestActivity = pageActivities.get(pageActivities.size() - 1);
            LocalDate oldestActivityDate = oldestActivity.getStartDateLocal().toLocalDate();

            // Check if we've reached the start of our date range
            if (after != null && (oldestActivityDate.isBefore(after) || oldestActivityDate.equals(after))) {
                break; // We've reached or passed our start date
            }

            // For the next iteration, set 'before' to one day before the oldest activity
            // This ensures we get the next batch of older activities
            currentBefore = oldestActivityDate.minusDays(1);
            iteration++;
        }

        // Filter activities based on startDateLocal to ensure we only include activities
        // that occurred within the specified date range in the athlete's local timezone
        return allActivities.stream()
            .filter(activity -> {
                LocalDate activityDate = activity.getStartDateLocal().toLocalDate();
                boolean afterCheck = after == null || !activityDate.isBefore(after);
                boolean beforeCheck = before == null || !activityDate.isAfter(before);
                return afterCheck && beforeCheck;
            })
            .toList();
    }
}
