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

  # --- Phase A: normal test suite (excludes CrashTest and PostCrashDeliveryTest)
  # Keeps clearPackageData=true so each non-crash test starts clean.
  set +e
  adb shell am instrument -w -m -r \
    -e clearPackageData true \
    -e notClass io.embrace.shoppingcart.CrashTest,io.embrace.shoppingcart.PostCrashDeliveryTest \
    "$FULL_APP_ID.test/androidx.test.runner.AndroidJUnitRunner" \
    | tee "result-$VARIANT-phaseA.txt"
  PHASE_A_EXIT=$?
  set -e

  # Check if emulator is still alive
  if ! adb devices | grep -q "emulator"; then
    echo "ERROR: Emulator died during Phase A for $VARIANT"
    exit 1
  fi

  # Clean slate before Phase B so the only payload on disk is the one
  # CrashTest produces.
  adb shell am force-stop "$FULL_APP_ID" || true
  adb shell pm clear "$FULL_APP_ID" || true

  # --- Phase B.1: crash test. NO clearPackageData — we need the crash payload
  # to survive on disk until the delivery test starts.
  set +e
  adb shell am instrument -w -m -r \
    -e class io.embrace.shoppingcart.CrashTest \
    "$FULL_APP_ID.test/androidx.test.runner.AndroidJUnitRunner" \
    | tee "result-$VARIANT-phaseB-crash.txt"
  PHASE_B_CRASH_EXIT=$?
  set -e

  if ! adb devices | grep -q "emulator"; then
    echo "ERROR: Emulator died during Phase B crash for $VARIANT"
    exit 1
  fi

  # CRITICAL: do not pm clear or force-stop here. The crash payload is sitting
  # on disk and the SDK will read it on next launch (which is Phase B.2).

  # --- Phase B.2: launch app and wait, so the Embrace SDK can ship the
  # pending crash payload left by Phase B.1.
  set +e
  adb shell am instrument -w -m -r \
    -e class io.embrace.shoppingcart.PostCrashDeliveryTest \
    "$FULL_APP_ID.test/androidx.test.runner.AndroidJUnitRunner" \
    | tee "result-$VARIANT-phaseB-delivery.txt"
  PHASE_B_DELIVERY_EXIT=$?
  set -e

  if ! adb devices | grep -q "emulator"; then
    echo "ERROR: Emulator died during Phase B delivery for $VARIANT"
    exit 1
  fi

  # Compose the variant's exit code: any phase failing marks the variant failed.
  TEST_EXIT_CODE=0
  [ $PHASE_A_EXIT -ne 0 ] && TEST_EXIT_CODE=$PHASE_A_EXIT
  [ $PHASE_B_CRASH_EXIT -ne 0 ] && TEST_EXIT_CODE=$PHASE_B_CRASH_EXIT
  [ $PHASE_B_DELIVERY_EXIT -ne 0 ] && TEST_EXIT_CODE=$PHASE_B_DELIVERY_EXIT

  # Now it is safe to clean up for the next variant.
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
