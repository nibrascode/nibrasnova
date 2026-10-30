#!/bin/sh
# Nibras Nova Gradle Wrapper Launcher
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "Please open this project in Android Studio (2024.2+) or install Gradle 8.9+ to run builds."
    exit 1
fi
