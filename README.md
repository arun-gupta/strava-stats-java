# Strava Activity Analyzer

A Spring Boot application for analyzing Strava activities with comprehensive statistics and visualizations.

## Features

- **Secure Strava OAuth Authentication** - Login with your Strava account
- **Activity Count Distribution** - Pie chart showing activity types with counts and percentages
- **Time Distribution** - Visualize time spent per activity type in HH:MM format
- **Workout Heatmap** - Heatmap tracking activities with intensity coloration
- **Run Statistics** - Comprehensive running metrics including:
  - Total runs and 10K+ runs count
  - Total miles and average pace
  - Personal records (fastest mile, fastest 10K, longest run, most elevation)
- **Running Heatmap** - Calendar view with color-coded daily mileage intensity
- **Mileage Trend** - Daily/Weekly/Monthly running mileage charts
- **Pace Trend** - Daily/Weekly/Monthly average pace displayed in MM:SS format
- **Date Range Filtering** - Filter all statistics by custom date ranges

## Prerequisites

### Java 21 Setup

This project requires Java 21. We recommend using SDKMAN! to manage Java versions.

#### Install SDKMAN!

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

#### Install and Set Java 21

```bash
# Install Amazon Corretto 21
sdk install java 21.0.9-amzn

# Set as default
sdk default java 21.0.9-amzn
```

#### Configure IDE

In IntelliJ IDEA:
1. Go to **Settings → Build, Execution, Deployment → Build Tools → Gradle**
2. Set **Gradle JVM** to Java 21

## Strava API Setup

1. Create a Strava API application at https://www.strava.com/settings/api
2. Set the Authorization Callback Domain to `localhost`
3. Note your **Client ID** and **Client Secret**

## Configuration

### Option 1: Local Properties File (Recommended for Development)

Create `src/main/resources/application-local.properties`:

```properties
spring.security.oauth2.client.registration.strava.client-id=your-client-id
spring.security.oauth2.client.registration.strava.client-secret=your-client-secret
```

This file is already in `.gitignore` and won't be committed to the repository.

### Option 2: Environment Variables

```bash
export STRAVA_CLIENT_ID=your-client-id
export STRAVA_CLIENT_SECRET=your-client-secret
```

### Option 3: Direct Configuration

Update `src/main/resources/application.properties` directly (not recommended - secrets will be committed to Git).

## Running the Application

If using `application-local.properties` (Option 1), run with the local profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

If using environment variables (Option 2), run:

```bash
./gradlew bootRun
```

The application will be available at http://localhost:8080

## Usage

1. Navigate to http://localhost:8080
2. Click "Connect with Strava" to authenticate
3. Authorize the application to access your Strava data
4. View your statistics on the dashboard
5. Use date range filters to analyze specific time periods

## API Endpoints

All endpoints require OAuth authentication and support optional `after` and `before` date parameters:

- `GET /api/stats/activity-count` - Activity count distribution
- `GET /api/stats/time-distribution` - Time spent per activity type
- `GET /api/stats/workout-heatmap` - Workout heatmap data
- `GET /api/stats/run-statistics` - Comprehensive running statistics
- `GET /api/stats/running-heatmap` - Running mileage heatmap data
- `GET /api/stats/mileage-trend?period={daily|weekly|monthly}` - Running mileage trends
- `GET /api/stats/pace-trend?period={daily|weekly|monthly}` - Running pace trends

## Technology Stack

- **Spring Boot 3.5.7** - Application framework
- **Spring Security OAuth2 Client** - Strava OAuth authentication
- **Spring WebFlux** - Reactive HTTP client for Strava API
- **Thymeleaf** - Server-side templating
- **Bootstrap 5** - UI framework
- **Chart.js** - Data visualization
- **Lombok** - Reduce boilerplate code