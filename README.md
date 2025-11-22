# Strava Activity Analyzer

A Spring Boot application for analyzing Strava activities with comprehensive statistics and visualizations.

## Quickstart

1. **Create a Strava API application**
   - Go to https://www.strava.com/settings/api
   - Create application with callback domain: `localhost`
   - Note your Client ID and Client Secret

2. **Configure credentials**
   ```bash
   cp src/main/resources/application.properties src/main/resources/application-local.properties
   # Edit application-local.properties and add your credentials
   ```

3. **Run** (opens http://localhost:8080 automatically)
   ```bash
   ./quickstart.sh
   ```

4. **Connect with Strava and start analyzing!**

> Requires Java 21. See [SETUP.md](SETUP.md) for detailed instructions.

## Features

- **Secure Strava OAuth Authentication** - Login with your Strava account
- **Activity Count** - Pie chart showing activity types with counts and percentages
- **Duration** - Visualize time spent per activity type in HH:MM format
- **Heatmap** - Toggle between:
  - **All Activities**: grid heatmap of any-activity days with streak counters and gap details
  - **Running**: calendar-like daily mileage heatmap with running streak counters
- **Trends** - Daily/Weekly/Monthly charts with smooth curves for:
  - Running mileage with tooltips
  - Average pace in MM:SS format
- **Running Stats** - Comprehensive running metrics including:
  - Total runs and 10K+ runs count
  - Total miles and average pace
  - Run distance distribution histogram (0-10 miles in 1-mile ranges)
  - Personal records (fastest mile, fastest 10K, longest run, most elevation)
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