package io.embrace.shoppingcart

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoActivityResumedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import io.embrace.shoppingcart.ui.home.HomeActivity
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashTest {

    @get:Rule
    val composeRule: AndroidComposeTestRule<*, *> = createAndroidComposeRule<HomeActivity>()




    @Test
    fun full_crash() {
        // Click "enter_as_guest" if present (skip if already authenticated as guest)
        composeRule.clickIfExists(tag = "enter_as_guest", timeoutMs = 3_000)

        // Add first product to cart
        composeRule.waitUntil(conditionDescription = "crash_button", 30_000) {
            composeRule.onAllNodes(hasTestTag("crash_button"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasTestTag("crash_button"), useUnmergedTree = true)[0]
            .performClick()

        repeat(3) { try { Espresso.pressBack(); Thread.sleep(300) } catch (_: NoActivityResumedException) { return@repeat } }
        Thread.sleep(2_000)

        // Relaunch the app after the crash
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        Thread.sleep(1_000)

        repeat(3) { try { Espresso.pressBack(); Thread.sleep(300) } catch (_: NoActivityResumedException) { return@repeat } }
        Thread.sleep(2_000)
        assert(true)
    }
}
