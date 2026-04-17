package io.embrace.shoppingcart

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.embrace.shoppingcart.ui.home.HomeActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Phase B of the two-phase crash pipeline. Phase A (CrashTest) leaves a
// pending crash payload on disk; this test relaunches the app so the Embrace
// SDK can read it on init and ship it. MUST run with no clearPackageData and
// no pm clear between Phase A and this test, otherwise the payload is wiped.
@RunWith(AndroidJUnit4::class)
class PostCrashDeliveryTest {

    @get:Rule
    val composeRule: AndroidComposeTestRule<*, *> = createAndroidComposeRule<HomeActivity>()

    @Test
    fun wait_for_pending_crash_upload() {
        Thread.sleep(15_000L)
        assert(true)
    }
}
