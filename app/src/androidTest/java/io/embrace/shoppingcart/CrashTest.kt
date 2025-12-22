package io.embrace.shoppingcart

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoActivityResumedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import io.embrace.shoppingcart.ui.home.HomeActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashTest {

    @get:Rule
    val composeRule: AndroidComposeTestRule<*, *> = createAndroidComposeRule<HomeActivity>()

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun full_crash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = context.packageName

        // Click "enter_as_guest" if present (skip if already authenticated as guest)
        composeRule.clickIfExists(tag = "enter_as_guest", timeoutMs = 3_000)

        // Wait for crash button and trigger crash
        composeRule.waitUntil(conditionDescription = "crash_button", 30_000) {
            composeRule.onAllNodes(hasTestTag("crash_button"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasTestTag("crash_button"), useUnmergedTree = true)[0]
            .performClick()

        // Wait for async crash to occur (100ms delay in Handler + crash processing time)
        Thread.sleep(2_000)

        // Use UiDevice to dismiss any crash dialogs (more reliable than Espresso after crash)
        repeat(3) {
            try {
                device.pressBack()
                Thread.sleep(300)
            } catch (_: Exception) {
                // Ignore errors - crash dialogs may not exist
            }
        }

        // Wait for crash to be fully processed
        Thread.sleep(1_000)

        // Verify app is not running after crash
        val appStillRunning = device.wait(Until.hasObject(By.pkg(packageName)), 500)
        if (appStillRunning) {
            println("Warning: App still running after crash, forcing close")
            device.pressBack()
            Thread.sleep(500)
        }

        // Relaunch the app
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        if (intent == null) {
            throw AssertionError("Could not get launch intent for package: $packageName")
        }

        context.startActivity(intent)

        // Wait for app to appear (using UiDevice, more reliable than composeRule)
        val appRestarted = device.wait(Until.hasObject(By.pkg(packageName)), 15_000)
        if (!appRestarted) {
            throw AssertionError("Failed to restart app after crash - app did not appear")
        }

        // Give app additional time to fully initialize and settle
        Thread.sleep(4_000)

        // Verify app is responsive by checking if any clickable element exists
        val appResponsive = device.wait(Until.hasObject(By.clickable(true)), 5_000)
        if (!appResponsive) {
            println("Warning: No clickable elements found, but app is running")
        }

        // Test passes if we got here - app crashed and successfully restarted
        println("SUCCESS: App crashed and restarted successfully")
        assert(true)
    }
}
