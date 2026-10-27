#!/usr/bin/env bash
#
# Build the WWW Leveler plugin (.nbm) against a fresh UGS source checkout.
# Requires: JDK 25 on PATH (or JAVA_HOME), git.
#
set -euo pipefail

TAG="${UGS_TAG:-v2.1.27}"
HERE="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$HERE/.ugs-build"

# --- JDK 25 check ---
if ! command -v javac >/dev/null 2>&1; then
  echo "ERROR: javac not found. Install JDK 25 and put it on PATH (or set JAVA_HOME)." >&2
  exit 1
fi
if ! javac -version 2>&1 | grep -qE ' 25(\.|$| )'; then
  echo "WARNING: javac is not version 25. UGS 2.1.x targets Java 25; the build may fail." >&2
  javac -version >&2 || true
fi

# --- fresh UGS checkout ---
if [ ! -d "$BUILD/.git" ]; then
  echo ">> cloning UGS $TAG ..."
  rm -rf "$BUILD"
  git clone --depth 1 --branch "$TAG" https://github.com/winder/Universal-G-Code-Sender.git "$BUILD"
else
  echo ">> reusing existing checkout at $BUILD"
fi

# --- inject this module ---
echo ">> injecting WWWLeveler module ..."
rm -rf "$BUILD/ugs-platform/WWWLeveler"
mkdir -p "$BUILD/ugs-platform/WWWLeveler"
cp -r "$HERE/plugin/." "$BUILD/ugs-platform/WWWLeveler/"

# --- register module in the platform parent pom (idempotent) ---
POM="$BUILD/ugs-platform/pom.xml"
if ! grep -q '<module>WWWLeveler</module>' "$POM"; then
  echo ">> registering module in $POM ..."
  # insert just before the closing </modules>
  perl -0pi -e 's#(\s*)</modules>#$1  <module>WWWLeveler</module>$1</modules>#' "$POM"
fi

# --- build ---
echo ">> building ..."
cd "$BUILD"
./mvnw install -pl ugs-platform/WWWLeveler -am -DskipTests -B

# --- collect artifact ---
mkdir -p "$HERE/dist"
cp "$BUILD"/ugs-platform/WWWLeveler/target/*.nbm "$HERE/dist/"
echo ""
echo "BUILD OK -> $(ls "$HERE"/dist/*.nbm)"
