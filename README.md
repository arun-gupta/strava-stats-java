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
- **Overview** - Pie chart showing activity types with counts and percentages
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
    <strong>Overview</strong><br>
    <a href="docs/images/overview.png"><img src="docs/images/overview.png" alt="Overview" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Duration</strong><br>
    <a href="docs/images/duration.png"><img src="docs/images/duration.png" alt="Duration" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Heatmap - All Activities</strong><br>
    <a href="docs/images/heatmap-activity.png"><img src="docs/images/heatmap-activity.png" alt="Heatmap - All Activities" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Heatmap - Running</strong><br>
    <a href="docs/images/heatmap-running.png"><img src="docs/images/heatmap-running.png" alt="Heatmap - Running" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Trends - Mileage</strong><br>
    <a href="docs/images/trend-mileage.png"><img src="docs/images/trend-mileage.png" alt="Trends - Mileage" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Trends - Pace</strong><br>
    <a href="docs/images/trend-pace.png"><img src="docs/images/trend-pace.png" alt="Trends - Pace" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
  </div>
  <div style="flex: 0 0 calc(33.333% - 14px); min-width: 250px; text-align: center;">
    <strong>Running Stats</strong><br>
    <a href="docs/images/running-stats.png"><img src="docs/images/running-stats.png" alt="Running Stats" style="width: 300px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
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