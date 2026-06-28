#!/bin/sh
set -e

APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
LOCAL_GRADLE="/opt/k2s/.tooling/gradle-8.7/bin/gradle"

if [ -x "$LOCAL_GRADLE" ]; then
  exec "$LOCAL_GRADLE" "$@"
fi

JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if ! command -v "$JAVA_CMD" >/dev/null 2>&1; then
  JAVA_CMD=java
fi

exec "$JAVA_CMD" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar:$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar:$APP_HOME/gradle/wrapper/gradle-cli.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
