#!/bin/sh
# Maven Wrapper Script
# https://github.com/apache/maven-wrapper

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

# OS specific support
case "$(uname -s)" in
CYGWIN* | MINGW* | MSYS*) native_path() { cygpath -w "$1"; } ;;
*) native_path() { printf '%s' "$1"; } ;;
esac

# Maven settings
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
export MAVEN_PROJECTBASEDIR
MAVEN_USER_HOME="${MAVEN_USER_HOME:-${HOME}/.m2}"

# Wrapper jar
MAVEN_WRAPPER_JAR="${MAVEN_PROJECTBASEDIR}/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="${MAVEN_PROJECTBASEDIR}/.mvn/wrapper/maven-wrapper.properties"

# Download wrapper jar if not present
if [ ! -f "${MAVEN_WRAPPER_JAR}" ]; then
    if [ -f "${MAVEN_WRAPPER_PROPERTIES}" ]; then
        wrapperUrl=$(grep '^wrapperUrl=' "${MAVEN_WRAPPER_PROPERTIES}" | cut -d= -f2-)
        if command -v curl > /dev/null 2>&1; then
            curl -fsSL -o "${MAVEN_WRAPPER_JAR}" "${wrapperUrl}"
        elif command -v wget > /dev/null 2>&1; then
            wget -q -O "${MAVEN_WRAPPER_JAR}" "${wrapperUrl}"
        else
            echo "Error: curl or wget required to download maven-wrapper.jar" >&2
            exit 1
        fi
    else
        echo "Error: ${MAVEN_WRAPPER_PROPERTIES} not found" >&2
        exit 1
    fi
fi

# Run Maven with proper system property for multi-module projects
exec java \
    ${MAVEN_OPTS-} \
    ${MAVEN_DEBUG_OPTS-} \
    -Dmaven.multiModuleProjectDirectory="${MAVEN_PROJECTBASEDIR}" \
    -classpath "${MAVEN_WRAPPER_JAR}" \
    org.apache.maven.wrapper.MavenWrapperMain \
    ${MAVEN_CONFIG-} \
    "$@"
