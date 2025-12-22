#!/usr/bin/env bash
set -e

adb devices

VARIANTS=(
  "mockDebug app-mockDebug.apk app-mock-demo-debug.apk app-mockDebug-androidTest.apk app-mock-demo-debug-androidTest.apk .mock.demo"
  "mockSandbox app-mockSandbox.apk app-mock-sandbox-debug.apk app-mockSandbox-androidTest.apk app-mock-sandbox-debug-androidTest.apk .mock.sandbox"
  "mockEurope app-mockEurope.apk app-mock-europe-debug.apk app-mockEurope-androidTest.apk app-mock-europe-debug-androidTest.apk .mock.europe"
  "prodDebug app-prodDebug.apk app-prod-demo-debug.apk app-prodDebug-androidTest.apk app-prod-demo-debug-androidTest.apk .demo"
)

FAILED_VARIANTS=()

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

  # Run tests and capture exit code without failing the script
  set +e
  adb shell am instrument -w -m -r \
    -e clearPackageData true \
    "$FULL_APP_ID.test/androidx.test.runner.AndroidJUnitRunner" \
    | tee "result-$VARIANT.txt"
  TEST_EXIT_CODE=$?
  set -e

  # Check if emulator is still alive
  if ! adb devices | grep -q "emulator"; then
    echo "ERROR: Emulator died during test execution for $VARIANT"
    exit 1
  fi

  # Force stop app to ensure clean state for next variant
  adb shell am force-stop "$FULL_APP_ID" || true
  sleep 2

  # Track failed variants but continue execution
  if [ $TEST_EXIT_CODE -ne 0 ]; then
    echo "WARNING: Tests failed for $VARIANT (exit code: $TEST_EXIT_CODE), but continuing with next variant"
    FAILED_VARIANTS+=("$VARIANT")
  fi
done

# Report all failures at the end
if [ ${#FAILED_VARIANTS[@]} -gt 0 ]; then
  echo ""
  echo "========================================"
  echo "Summary: ${#FAILED_VARIANTS[@]} variant(s) had test failures:"
  for FAILED_VAR in "${FAILED_VARIANTS[@]}"; do
    echo "  - $FAILED_VAR"
  done
  echo "========================================"
  # Exit with error so the workflow is marked as failed, but all tests ran
  exit 1
fi

echo "All variants passed successfully!"
