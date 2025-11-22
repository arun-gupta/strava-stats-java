# Strava Activity Analyzer

A Spring Boot application for analyzing Strava activities with comprehensive statistics and visualizations.

## Quickstart

Get up and running in 5 minutes:

1. **Create a Strava API application**
   - Go to https://www.strava.com/settings/api
   - Create a new application with callback domain: `localhost`
   - Note your Client ID and Client Secret

2. **Set up credentials (local profile)**
   Use the provided template. `quickstart.sh` will look for credentials in this order:
   1) `src/main/resources/application-local.properties` (preferred)
   2) Environment variables (`STRAVA_CLIENT_ID`, `STRAVA_CLIENT_SECRET`)
   ```bash
   # 2a) Create a local profile config from the template
   cp src/main/resources/application.properties src/main/resources/application-local.properties

   # 2b) Option A — set credentials in application-local.properties (preferred)
   # Open the file and set the Spring OAuth properties, for example:
   # spring.security.oauth2.client.registration.strava.client-id=your-client-id
   # spring.security.oauth2.client.registration.strava.client-secret=your-client-secret

   # 2c) Option B — use environment variables (fallback if properties are not set)
   # export STRAVA_CLIENT_ID=your-client-id
   # export STRAVA_CLIENT_SECRET=your-client-secret
   ```

   Notes:
   - You can keep secrets in the properties file (preferred for local dev) or in env vars (useful for CI/temporary shells). The script will pick up whichever is available based on the precedence above.
   - If you use environment variables only, you don’t have to edit the properties file; `application.properties` already contains `${STRAVA_CLIENT_ID}` and `${STRAVA_CLIENT_SECRET}` placeholders.
   - Avoid committing real credentials to source control.

3. **Run the application**
   Fastest way:
   ```bash
   ./quickstart.sh
   ```
   Or, via Gradle:
   ```bash
   ./gradlew bootRun -Dspring-boot.run.profiles=local
   ```

4. **Access the dashboard**
   - Open http://localhost:8080
   - Click "Connect with Strava"
   - Authorize the application
   - Start analyzing your activities!

> **Note:** Requires Java 21. For detailed setup instructions, see [SETUP.md](SETUP.md).

## Features

- **Secure Strava OAuth Authentication** - Login with your Strava account
- **Activity Count Distribution** - Pie chart showing activity types with counts and percentages
- **Time Distribution** - Visualize time spent per activity type in HH:MM format
- **Streaks (toggleable)** - One tab with a toggle to switch between:
  - **All Activities** (formerly Workout Heatmap): grid heatmap of any-activity days with streak counters and gap details
  - **Running** (formerly Running Heatmap): calendar-like daily mileage heatmap with running streak counters
- **Running Stats** - Comprehensive running metrics including:
  - Total runs and 10K+ runs count
  - Total miles and average pace
  - Run distance distribution histogram (0-10 miles in 1-mile ranges)
  - Personal records (fastest mile, fastest 10K, longest run, most elevation)
- **Mileage Trend** - Daily/Weekly/Monthly running mileage charts with smooth curves and tooltips
- **Pace Trend** - Daily/Weekly/Monthly average pace displayed in MM:SS format
- **Date Range Filtering** - Quick pick buttons (7 days, 30 days, 90 days, 6 months, 1 year, YTD, All Time) and custom date range selection

## Screenshots

Click on any thumbnail to view the full image:

<div style="display: flex; flex-wrap: wrap; gap: 20px; justify-content: center;">
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Activity Count</strong><br>
    <a href="docs/images/activity-count.png"><img src="docs/images/activity-count.png" alt="Activity Count" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Time Distribution</strong><br>
    <a href="docs/images/time-distribution.png"><img src="docs/images/time-distribution.png" alt="Time Distribution" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Workout Heatmap</strong><br>
    <a href="docs/images/workout-heatmap.png"><img src="docs/images/workout-heatmap.png" alt="Workout Heatmap" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Running Heatmap</strong><br>
    <a href="docs/images/running-heatmap.png"><img src="docs/images/running-heatmap.png" alt="Running Heatmap" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Running Stats</strong><br>
    <a href="docs/images/running-stats.png"><img src="docs/images/running-stats.png" alt="Running Stats" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Mileage Trend</strong><br>
    <a href="docs/images/mileage-trend.png"><img src="docs/images/mileage-trend.png" alt="Mileage Trend" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Pace Trend</strong><br>
    <a href="docs/images/pace-trend.png"><img src="docs/images/pace-trend.png" alt="Pace Trend" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
</div>

## Documentation

For detailed setup instructions, configuration options, API documentation, and troubleshooting, see [SETUP.md](SETUP.md).

To recreate this application in any language or stack, see the language-agnostic [requirements.md](docs/requirements.md).

## Technology Stack

- **Spring Boot 3.5.7** - Application framework
- **Spring Security OAuth2 Client** - Strava OAuth authentication
- **Spring WebFlux** - Reactive HTTP client for Strava API
- **Thymeleaf** - Server-side templating
- **Bootstrap 5** - UI framework
- **Chart.js** - Data visualization
- **Lombok** - Reduce boilerplate code