#!/bin/sh

APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "$( dirname "$0" )" >/dev/null 2>&1 && pwd )

exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
