package com.example.strava.service;

import com.example.strava.model.StravaActivity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class StravaApiService {

    private final WebClient webClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public StravaApiService(
            @Value("${strava.api.base-url}") String baseUrl,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.authorizedClientService = authorizedClientService;
    }

    public List<StravaActivity> getActivities(String principalName, LocalDate after, LocalDate before, int perPage) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("strava", principalName);

        if (client == null) {
            throw new IllegalStateException("No authorized client found for: " + principalName);
        }

        OAuth2AccessToken accessToken = client.getAccessToken();

        ZoneId timezone = ZoneId.systemDefault();
        long afterEpoch = after != null ? after.atStartOfDay(timezone).toEpochSecond() : 0;
        // Strava's 'before' parameter is exclusive, so use end of day in user's timezone + 1 second
        // This ensures all activities on the 'before' date are included, regardless of timezone
        long beforeEpoch = before != null 
            ? before.atTime(LocalTime.MAX).atZone(timezone).toEpochSecond() + 1
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

    public List<StravaActivity> getAllActivities(String principalName, LocalDate after, LocalDate before) {
        // Fetch up to 200 activities (Strava's max per page is 200)
        return getActivities(principalName, after, before, 200);
    }
}
