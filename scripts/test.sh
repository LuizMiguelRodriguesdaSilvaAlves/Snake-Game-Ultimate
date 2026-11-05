#!/usr/bin/env sh
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BUILD_DIR="$PROJECT_ROOT/out"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

find "$PROJECT_ROOT/src" -name '*.java' -print > "$BUILD_DIR/sources.txt"
javac --release 17 -encoding UTF-8 -d "$BUILD_DIR" @"$BUILD_DIR/sources.txt"
javac --release 17 -encoding UTF-8 -cp "$BUILD_DIR" -d "$BUILD_DIR" "$PROJECT_ROOT/tests/ModeSystemSmokeTest.java"
java -ea -Djava.awt.headless=true -cp "$BUILD_DIR" ModeSystemSmokeTest
