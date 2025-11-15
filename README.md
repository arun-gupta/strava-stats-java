# strava-stats-java

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