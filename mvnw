#!/usr/bin/env sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_VERSION=3.9.11
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"
ARCHIVE="$WRAPPER_DIR/dists/apache-maven-$MAVEN_VERSION-bin.tar.gz"
DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$WRAPPER_DIR/dists"

  if [ ! -f "$ARCHIVE" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DIST_URL" -o "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$DIST_URL" -O "$ARCHIVE"
    else
      echo "Neither curl nor wget is available to download Maven $MAVEN_VERSION." >&2
      exit 1
    fi
  fi

  rm -rf "$MAVEN_HOME"
  tar -xzf "$ARCHIVE" -C "$WRAPPER_DIR"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
