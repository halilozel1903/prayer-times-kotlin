#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# The sample opens each screen from the `scene` extra, switches to the scene's language with the in-app language
# switch (AppCompatDelegate.setApplicationLocales), and loads today's prayer times LIVE from api.aladhan.com.
# There is no fallback data: a scene that shows an error or does not finish loading logs SCENE_FAILED or times
# out, and the run fails. Every capture is also checked for the expected text and for a blank image.
#
#   bash scripts/screenshots.sh phone    # pixel_7: today (TR, EN), the 81 il picker, the month, the Qibla
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: rail, province list and today side by side
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"

# The text each scene must show; the capture fails without it. None of these appear on the error screen.
expected_text() {
  case "$1" in
    today-tr) echo "Sonraki vakit" ;;
    today-en) echo "Next prayer" ;;
    city-picker-tr) echo "Afyonkarahisar" ;;
    month) echo "İkindi" ;;
    qibla) echo "Aladhan" ;;
    tablet) echo "Next prayer" ;;
  esac
}

# The countdown on screen is computed from the real time, so the status bar shows the real time in Turkey too
# (System UI demo mode would otherwise show 09:41).
real_clock() {
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm "$(TZ=Europe/Istanbul date +%H%M)" > /dev/null
}

install_sample
if [ "$device" = tablet ]; then
  ensure_landscape
  set_night_mode light
  fresh_launch --es scene tablet
  wait_for_scene tablet
  real_clock
  capture "tablet" "$(expected_text tablet)"
else
  set_night_mode light
  for scene in today-tr today-en city-picker-tr month qibla; do
    fresh_launch --es scene "$scene"
    wait_for_scene "$scene"
    real_clock
    capture "phone-$scene" "$(expected_text "$scene")"
  done
  # One dark capture of the main screen.
  set_night_mode dark
  fresh_launch --es scene today-tr
  wait_for_scene today-tr
  real_clock
  capture "phone-today-tr-dark" "$(expected_text today-tr)"
fi
