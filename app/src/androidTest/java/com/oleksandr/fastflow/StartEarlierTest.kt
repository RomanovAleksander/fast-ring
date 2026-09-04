package com.oleksandr.fastflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A fast usually starts before anyone presses a button, so Home has to offer
 * the real moment rather than only "now".
 */
@RunWith(AndroidJUnit4::class)
class StartEarlierTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun isShowing(id: Int): Boolean =
        composeRule.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty()

    /** Asserted before the tap, so a row that fell off the screen says so. */
    private fun tap(id: Int) {
        val node = composeRule.onAllNodesWithText(text(id)).onFirst()
        node.assertIsDisplayed()
        node.performClick()
        composeRule.waitForIdle()
    }

    /** Rethrows a timeout with the semantics tree, which says what *is* on screen. */
    private fun await(id: Int) {
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) { isShowing(id) }
        } catch (timeout: Throwable) {
            throw AssertionError(
                "Never saw \"${text(id)}\". Semantics tree:\n" +
                    composeRule.onRoot().printToString(maxDepth = 6),
                timeout,
            )
        }
    }

    @Before
    fun startFromHome() {
        if (isShowing(R.string.action_next)) {
            composeRule.onNodeWithText(text(R.string.action_next)).performClick()
            composeRule.onNodeWithText(text(R.string.action_done)).performClick()
        }
        await(R.string.action_start)
    }

    @Test
    fun theStartTimeCanBePickedBeforeStarting() {
        tap(R.string.action_start_earlier)
        await(R.string.home_start_at)

        // Scrolled to first: on a short screen the sheet's buttons sit below
        // the clock dial, which is exactly why the sheet scrolls at all.
        composeRule.onNodeWithText(text(R.string.action_cancel))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        await(R.string.action_start_earlier)
    }
}
