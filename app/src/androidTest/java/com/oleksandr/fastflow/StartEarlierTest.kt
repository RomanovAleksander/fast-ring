package com.oleksandr.fastflow

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
    fun theStartTimeCanBePickedBeforeStarting() {
        tap(R.string.action_start_earlier)
        composeRule.waitUntil(timeoutMillis = 5_000) { isShowing(R.string.home_start_at) }

        // Cancelling starts nothing, so the timer tab is untouched afterwards.
        tap(R.string.action_cancel)
        composeRule.waitUntil(timeoutMillis = 5_000) { isShowing(R.string.action_start_earlier) }
    }
}
