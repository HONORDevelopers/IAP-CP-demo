#!/usr/bin/env bash

echo "build.sh start"

./gradlew clean


./gradlew :app:assembleDebug \
:app:assembleRelease
