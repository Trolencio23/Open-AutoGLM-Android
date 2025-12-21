#!/bin/sh

##############################################################################
#
#   Gradle start up script for POSIX
#
##############################################################################

# Attempt to set APP_HOME
app_path=$0
while [ -h "$app_path" ]; do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in
      /*) app_path=$link ;;
      *) app_path=$( dirname "$app_path" )/$link ;;
    esac
done

APP_HOME=$( cd -P "$( dirname "$app_path" )" && pwd )
APP_NAME="Gradle"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if ! command -v "$JAVACMD" > /dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    exit 1
fi

# Execute Gradle
exec "$JAVACMD" \
    -Xmx64m -Xms64m \
    -Dorg.gradle.appname="$APP_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
