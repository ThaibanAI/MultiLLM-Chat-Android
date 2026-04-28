#!/bin/bash

# Gradle wrapper - downloads the wrapper jar if missing
APP_HOME=$( cd "${BASH_SOURCE[0]%/*}" > /dev/null && pwd -P ) || exit

# Determine the Java command
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Download wrapper jar if missing
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle Wrapper JAR..."
    # Extract the version from properties
    GRADLE_VERSION=$(grep distributionUrl "$WRAPPER_PROPS" | sed 's/.*gradle-\([0-9.]*\)-bin\.zip.*/\1/')
    DOWNLOAD_URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"
    # Try to download from Maven Central
    DOWNLOAD_URL="https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/${GRADLE_VERSION}/gradle-wrapper-${GRADLE_VERSION}.jar"
    
    # Actually use a well-known URL for the wrapper jar
    DOWNLOAD_URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"
    
    if command -v curl &> /dev/null; then
        curl -sL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" --output /dev/null && \
        curl -sL "https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar" -o "$WRAPPER_JAR"
    elif command -v wget &> /dev/null; then
        wget -q "https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar" -O "$WRAPPER_JAR"
    fi
    
    if [ ! -f "$WRAPPER_JAR" ]; then
        echo "ERROR: Could not download Gradle wrapper JAR. Please add it manually."
        exit 1
    fi
fi

exec "$JAVACMD" \
    $DEFAULT_JVM_OPTS \
    $JAVA_OPTS \
    $GRADLE_OPTS \
    "-Dorg.gradle.appname=$(basename "$0")" \
    -classpath "$WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain "$@"
