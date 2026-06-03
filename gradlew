#!/bin/bash
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/SDK}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
CLASSPATH="$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar"
"$JAVA_HOME/bin/java" $DEFAULT_JVM_OPTS $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
