# Detailed Setup Guide

This guide provides detailed instructions for setting up the Strava Activity Analyzer application.

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

## Configuration Options

### Option 1: Local Properties File (Recommended for Development)

Create `src/main/resources/application-local.properties`:

```properties
spring.security.oauth2.client.registration.strava.client-id=your-client-id
spring.security.oauth2.client.registration.strava.client-secret=your-client-secret
```

This file is already in `.gitignore` and won't be committed to the repository.

**Running with local properties:**
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Option 2: Environment Variables

```bash
export STRAVA_CLIENT_ID=your-client-id
export STRAVA_CLIENT_SECRET=your-client-secret
```

**Running with environment variables:**
```bash
./gradlew bootRun
```

### Option 3: Direct Configuration

Update `src/main/resources/application.properties` directly (not recommended - secrets will be committed to Git).

## Running the Application

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
- `GET /api/stats/run-distribution` - Run distance distribution histogram
- `GET /api/stats/running-heatmap` - Running mileage heatmap data
- `GET /api/stats/mileage-trend?period={daily|weekly|monthly}` - Running mileage trends
- `GET /api/stats/pace-trend?period={daily|weekly|monthly}` - Running pace trends

## Troubleshooting

### Java Version Issues

If you encounter Java version errors:
- Verify Java 21 is installed: `java -version`
- Check JAVA_HOME: `echo $JAVA_HOME`
- Set JAVA_HOME if needed: `export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.9-amzn`

### OAuth Authentication Issues

If authentication fails:
- Verify your Strava API credentials are correct
- Check that the callback domain is set to `localhost`
- Ensure the application is running on port 8080

### Port Already in Use

If port 8080 is already in use, you can change it by setting:
```bash
export SERVER_PORT=8081
```

Or add to your properties file:
```properties
server.port=8081
```
