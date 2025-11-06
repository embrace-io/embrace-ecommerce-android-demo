#!/usr/bin/env bash
set -e

adb devices

VARIANTS=(
  "mockDebug app-mockDebug.apk app-mock-demo-debug.apk app-mockDebug-androidTest.apk app-mock-demo-debug-androidTest.apk .mock.demo"
  "mockSandbox app-mockSandbox.apk app-mock-sandbox-debug.apk app-mockSandbox-androidTest.apk app-mock-sandbox-debug-androidTest.apk .mock.sandbox"
  "mockEurope app-mockEurope.apk app-mock-europe-debug.apk app-mockEurope-androidTest.apk app-mock-europe-debug-androidTest.apk .mock.europe"
  "prodDebug app-prodDebug.apk app-prod-demo-debug.apk app-prodDebug-androidTest.apk app-prod-demo-debug-androidTest.apk .demo"
)

for ENTRY in "${VARIANTS[@]}"; do
  read VARIANT ARTIFACT_APP FILE_APP ARTIFACT_TEST FILE_TEST SUFFIX <<< "$ENTRY"

  mkdir -p "tmp/$VARIANT/app"
  mkdir -p "tmp/$VARIANT/test"

  cp "all_artifacts/$ARTIFACT_APP/$FILE_APP" "tmp/$VARIANT/app/"
  cp "all_artifacts/$ARTIFACT_TEST/$FILE_TEST" "tmp/$VARIANT/test/"

  APP_APK="tmp/$VARIANT/app/$FILE_APP"
  TEST_APK="tmp/$VARIANT/test/$FILE_TEST"
  FULL_APP_ID="${APP_ID}${SUFFIX}"

  adb install -r "$APP_APK"
  adb install -r "$TEST_APK"
  adb shell pm clear "$FULL_APP_ID" || true
  adb shell am instrument -w -m -r \
    -e clearPackageData true \
    "$FULL_APP_ID.test/androidx.test.runner.AndroidJUnitRunner" \
    | tee "result-$VARIANT.txt"
done
