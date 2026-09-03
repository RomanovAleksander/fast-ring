package com.oleksandr.fastflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pausing has to survive the round trip through DataStore and back into the
 * state machine, which is exactly the part a unit test cannot reach.
 *
 * Both labels live in the bottom action area, so neither can be scrolled out
 * of the semantics tree the way list content can.
 */
@RunWith(AndroidJUnit4::class)
class PauseTrackingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun isShowing(id: Int): Boolean =
        composeRule.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty()

    private fun tap(id: Int) {
        composeRule.onAllNodesWithText(text(id)).onFirst().performClick()
        composeRule.waitForIdle()
    }

    @Before
    fun startFromHome() {
        if (isShowing(R.string.action_next)) {
            composeRule.onNodeWithText(text(R.string.action_next)).performClick()
            composeRule.onNodeWithText(text(R.string.action_done)).performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { isShowing(R.string.action_start) }
    }

    @Test
    fun pausingAndResumingSwapsTheSecondaryAction() {
        tap(R.string.action_pause)
        composeRule.waitUntil(timeoutMillis = 5_000) { isShowing(R.string.action_resume_tracking) }

        // Starting is still offered while paused: it is itself a resume.
        composeRule.onNodeWithText(text(R.string.action_start)).assertIsDisplayed()

        // Leaves the app unpaused, so the flag cannot leak into other tests.
        tap(R.string.action_resume_tracking)
        composeRule.waitUntil(timeoutMillis = 5_000) { isShowing(R.string.action_pause) }
    }
}
