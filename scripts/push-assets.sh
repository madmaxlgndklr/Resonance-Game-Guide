#!/usr/bin/env bash
# Sideload maps and sprites onto a connected device without rebuilding the APK.
# The app checks Android/data/<package>/files/guide/ before its bundled assets.
#
# Usage: scripts/push-assets.sh [assets-dir]
set -euo pipefail

PACKAGE="com.madmaxlgndklr.resonanceguide"
SRC="${1:-/home/madmaxlgndklr/Git/Resonance-Assets}"
DEST="/sdcard/Android/data/${PACKAGE}/files/guide"

if [[ ! -d "$SRC" ]]; then
  echo "Asset directory not found: $SRC" >&2
  exit 1
fi

adb shell mkdir -p "$DEST"
adb push --sync "$SRC/." "$DEST/"
echo "Pushed $SRC -> $DEST"
