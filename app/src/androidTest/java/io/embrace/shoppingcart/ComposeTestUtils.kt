package io.embrace.shoppingcart

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.AndroidComposeTestRule

/**
 * Clicks the first node with the given test tag if it appears within [timeoutMs].
 * If the node never appears, it silently continues without clicking.
 */
fun AndroidComposeTestRule<*, *>.clickIfExists(tag: String, timeoutMs: Long = 3_000L) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val nodes = onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            onAllNodes(hasTestTag(tag), useUnmergedTree = true)[0].performClick()
            return
        }
        Thread.sleep(100)
    }
}

/** Returns true if any node with [tag] exists right now. */
fun AndroidComposeTestRule<*, *>.exists(tag: String): Boolean {
    return try {
        onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    } catch (_: AssertionError) {
        false
    }
}
