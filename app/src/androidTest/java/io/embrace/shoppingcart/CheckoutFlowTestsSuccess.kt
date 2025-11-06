package io.embrace.shoppingcart

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoActivityResumedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.embrace.shoppingcart.ui.home.HomeActivity
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckoutFlowTestsSuccess {

    @get:Rule
    val composeRule: AndroidComposeTestRule<*, *> = createAndroidComposeRule<HomeActivity>()


    companion object Companion {
        @JvmStatic
        @BeforeClass
        fun setupClass() {
            io.embrace.shoppingcart.presentation.testutil.UiTestOverrides.verticalListForTests = true
            io.embrace.shoppingcart.mock.MockNetworkConfigOverrides.override =
                io.embrace.shoppingcart.mock.MockNetworkConfig(
                    defaultDelayMs = 0L,
                    slowDelayMs = 3000L,
                    productsScenario = io.embrace.shoppingcart.mock.NetworkScenario.SUCCESS,
                    categoriesScenario = io.embrace.shoppingcart.mock.NetworkScenario.SUCCESS,
                    placeOrderScenario = io.embrace.shoppingcart.mock.NetworkScenario.SUCCESS,
                )
        }
    }

    @Test
    fun full_checkout_happy_path() {
        // Click "enter_as_guest" if present (skip if already authenticated as guest)
        composeRule.clickIfExists(tag = "enter_as_guest", timeoutMs = 3_000)

        // Add first product to cart
        composeRule.waitUntil(conditionDescription = "add_to_cart", 30_000) {
            composeRule.onAllNodes(hasTestTag("add_to_cart"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasTestTag("add_to_cart"), useUnmergedTree = true)[0]
            .performClick()

        // Open cart
        composeRule.waitUntil(conditionDescription = "open_cart_btn", 10_000) {
            try {
                composeRule.onNode(hasTestTag("open_cart_btn"), useUnmergedTree = true)
                    .fetchSemanticsNode()
                true
            } catch (_: AssertionError) { false }
        }
        composeRule.onNode(hasTestTag("open_cart_btn"), useUnmergedTree = true).performClick()

        // Continue to checkout
        composeRule.waitUntil(conditionDescription = "cart_item", 30_000) {
            try { composeRule.onNode(hasTestTag("cart_item"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
        }
        composeRule.onNode(hasTestTag("checkout_btn"), useUnmergedTree = true).performClick()

        // Next: Shipping
        composeRule.waitUntil(conditionDescription = "to_shipping_btn", 10_000) {
            try { composeRule.onNode(hasTestTag("to_shipping_btn"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
        }
        composeRule.onNode(hasTestTag("to_shipping_btn"), useUnmergedTree = true).performClick()

        // Fill shipping fields
        composeRule.onNode(hasTestTag("name_field"), useUnmergedTree = true).performTextInput("John Tester")
        composeRule.onNode(hasTestTag("street_field"), useUnmergedTree = true).performTextInput("123 Test St")
        composeRule.onNode(hasTestTag("city_field"), useUnmergedTree = true).performTextInput("Testville")
        composeRule.onNode(hasTestTag("state_field"), useUnmergedTree = true).performTextInput("TS")
        composeRule.onNode(hasTestTag("zip_field"), useUnmergedTree = true).performTextInput("12345")
        composeRule.onNode(hasTestTag("country_field"), useUnmergedTree = true).performTextInput("Testland")

        // Next: Payment
        composeRule.onNode(hasTestTag("to_payment_btn"), useUnmergedTree = true).performClick()

        // Add payment method if needed (skip if one already exists)
        if (composeRule.exists("add_payment_btn")) {
            composeRule.onNode(hasTestTag("add_payment_btn"), useUnmergedTree = true).performClick()

            // Fill payment method details
            composeRule.waitUntil(conditionDescription = "brand_field", 10_000) {
                try { composeRule.onNode(hasTestTag("brand_field"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
            }
            composeRule.onNode(hasTestTag("brand_field"), useUnmergedTree = true).performTextInput("VISA")
            composeRule.onNode(hasTestTag("last4_field"), useUnmergedTree = true).performTextInput("4242")
            composeRule.onNode(hasTestTag("month_field"), useUnmergedTree = true).performTextInput("12")
            composeRule.onNode(hasTestTag("year_field"), useUnmergedTree = true).performTextInput("2028")
            composeRule.onNode(hasTestTag("save_btn"), useUnmergedTree = true).performClick()

            // Go back to Payment screen (dismiss keyboard then activity)
            Espresso.pressBack()
            Thread.sleep(200)
            Espresso.pressBack()
        }

        // Select the newly added payment method (row button)
        composeRule.waitUntil(conditionDescription = "visa_added", 15_000) {
            composeRule.onAllNodes(hasTestTag("visa_added"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasTestTag("visa_added"), useUnmergedTree = true)[0].performClick()

        // Next: Confirm
        composeRule.waitUntil(conditionDescription = "to_confirm_btn", 15_000) {
            try { composeRule.onNode(hasTestTag("to_confirm_btn"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
        }
        composeRule.onNode(hasTestTag("to_confirm_btn"), useUnmergedTree = true).performClick()

        // Place Order
        composeRule.waitUntil(conditionDescription = "place_order_btn", 10_000) {
            try { composeRule.onNode(hasTestTag("place_order_btn"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
        }
        composeRule.onNode(hasTestTag("place_order_btn"), useUnmergedTree = true).performClick()

        // Optionally, wait for confirmation text
        composeRule.waitUntil(conditionDescription = "order_placed", 30_000) {
            try { composeRule.onNode(hasTestTag("order_placed"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
        }

        composeRule.onNode(hasTestTag("finish_btn"), useUnmergedTree = true).performClick()
        repeat(3) { try { Espresso.pressBack(); Thread.sleep(300) } catch (_: NoActivityResumedException) { return@repeat } }
        Thread.sleep(2_000)
        assert(true)
    }



    @Test
    fun full_checkout_abandoned() {
        // Click "enter_as_guest" if present (skip if already authenticated as guest)
        composeRule.clickIfExists(tag = "enter_as_guest", timeoutMs = 3_000)

        // Add first product to cart
        composeRule.waitUntil(conditionDescription = "add_to_cart", 30_000) {
            composeRule.onAllNodes(hasTestTag("add_to_cart"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasTestTag("add_to_cart"), useUnmergedTree = true)[0]
            .performClick()

        // Open cart
        composeRule.waitUntil(conditionDescription = "open_cart_btn", 10_000) {
            try {
                composeRule.onNode(hasTestTag("open_cart_btn"), useUnmergedTree = true)
                    .fetchSemanticsNode()
                true
            } catch (_: AssertionError) { false }
        }
        composeRule.onNode(hasTestTag("open_cart_btn"), useUnmergedTree = true).performClick()

        // Continue to checkout
        composeRule.waitUntil(conditionDescription = "cart_item", 30_000) {
            try { composeRule.onNode(hasTestTag("cart_item"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
        }
        composeRule.onNode(hasTestTag("checkout_btn"), useUnmergedTree = true).performClick()

        // Next: Shipping
        composeRule.waitUntil(conditionDescription = "to_shipping_btn", 10_000) {
            try { composeRule.onNode(hasTestTag("to_shipping_btn"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
        }
        composeRule.onNode(hasTestTag("to_shipping_btn"), useUnmergedTree = true).performClick()

        // Fill shipping fields
        composeRule.onNode(hasTestTag("name_field"), useUnmergedTree = true).performTextInput("John Tester")
        composeRule.onNode(hasTestTag("street_field"), useUnmergedTree = true).performTextInput("123 Test St")
        composeRule.onNode(hasTestTag("city_field"), useUnmergedTree = true).performTextInput("Testville")
        composeRule.onNode(hasTestTag("state_field"), useUnmergedTree = true).performTextInput("TS")
        composeRule.onNode(hasTestTag("zip_field"), useUnmergedTree = true).performTextInput("12345")
        composeRule.onNode(hasTestTag("country_field"), useUnmergedTree = true).performTextInput("Testland")

        // Next: Payment
        composeRule.onNode(hasTestTag("to_payment_btn"), useUnmergedTree = true).performClick()

        // Add payment method if needed (skip if one already exists)
        if (composeRule.exists("add_payment_btn")) {
            composeRule.onNode(hasTestTag("add_payment_btn"), useUnmergedTree = true).performClick()

            // Fill payment method details
            composeRule.waitUntil(conditionDescription = "brand_field", 10_000) {
                try { composeRule.onNode(hasTestTag("brand_field"), useUnmergedTree = true).fetchSemanticsNode(); true } catch (_: AssertionError) { false }
            }
            composeRule.onNode(hasTestTag("brand_field"), useUnmergedTree = true).performTextInput("VISA")
            composeRule.onNode(hasTestTag("last4_field"), useUnmergedTree = true).performTextInput("4242")
            composeRule.onNode(hasTestTag("month_field"), useUnmergedTree = true).performTextInput("12")
            composeRule.onNode(hasTestTag("year_field"), useUnmergedTree = true).performTextInput("2028")
            composeRule.onNode(hasTestTag("save_btn"), useUnmergedTree = true).performClick()

            // Go back to Payment screen (dismiss keyboard then activity)
            Espresso.pressBack()
            Thread.sleep(200)
            Espresso.pressBack()
        }

        // Select the newly added payment method (row button)
        composeRule.waitUntil(conditionDescription = "visa_added", 15_000) {
            composeRule.onAllNodes(hasTestTag("visa_added"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasTestTag("visa_added"), useUnmergedTree = true)[0].performClick()

        repeat(3) { try { Espresso.pressBack(); Thread.sleep(300) } catch (_: NoActivityResumedException) { return@repeat } }
        Thread.sleep(2_000)
        assert(true)
    }
}
