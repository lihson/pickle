#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
javac -d "$OUT" android/app/src/main/java/tw/pickle/score/ScoreRules.java android/app/src/main/java/tw/pickle/score/Snapshot.java tests/ScoreTest.java
java -cp "$OUT" ScoreTest
