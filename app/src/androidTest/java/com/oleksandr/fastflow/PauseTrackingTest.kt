package com.oleksandr.fastflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pausing has to survive the round trip through DataStore and back into the
 * state machine, which is exactly the part a unit test cannot reach.
 *
 * It also guards the bottom of the screen: the pause action is the last thing
 * in the column, so it is the first to be pushed under the tab bar when
 * anything above it grows.
 */
@RunWith(AndroidJUnit4::class)
class PauseTrackingTest {

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
    fun pausingAndResumingSwapsTheSecondaryAction() {
        tap(R.string.action_pause)
        await(R.string.action_resume_tracking)

        // Starting is still offered while paused: it is itself a resume.
        composeRule.onNodeWithText(text(R.string.action_start)).assertIsDisplayed()

        // Leaves the app unpaused, so the flag cannot leak into other tests.
        tap(R.string.action_resume_tracking)
        await(R.string.action_pause)
    }
}
