#!/usr/bin/env bash
# Portable launcher for Linux / macOS: run this file to start the game.
# It looks for the jar next to this script or in the dist folder.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR=""

for candidate in "$HERE"/dist/java-snake-*.jar "$HERE"/java-snake-*.jar; do
  if [ -f "$candidate" ]; then
    JAR="$candidate"
    break
  fi
done

if [ -z "$JAR" ]; then
  echo "[java-snake] Cannot find the jar." >&2
  echo "[java-snake] Build it with 'mvn package' then copy it into dist/, or download it from GitHub Releases." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[java-snake] 'java' is not on PATH. Please install JDK 17 or newer." >&2
  exit 1
fi

exec java -jar "$JAR"
