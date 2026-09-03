package com.oleksandr.fastflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the app actually starts on a device.
 *
 * Compiling says nothing about whether Hilt can build its graph, whether Room
 * opens the database, or whether the first frame renders — all of which happen
 * only at runtime, and all of which would crash on launch.
 *
 * Onboarding completion is persisted, so these assertions never assume which
 * of the two first screens is showing.
 */
@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun isShowing(id: Int): Boolean =
        composeRule.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theAppLaunchesAndRendersItsFirstScreen() {
        val onboarding = isShowing(R.string.onboarding_1_title)
        val home = isShowing(R.string.tab_timer)
        assert(onboarding || home) { "neither onboarding nor the tab bar rendered" }
    }

    @Test
    fun onboardingEndsOnTheTimerTab() {
        if (isShowing(R.string.action_next)) {
            composeRule.onNodeWithText(text(R.string.action_next)).performClick()
            composeRule.onNodeWithText(text(R.string.onboarding_2_title)).assertIsDisplayed()
            composeRule.onNodeWithText(text(R.string.action_done)).performClick()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.tab_timer)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.weekday_mon)).assertIsDisplayed()
    }
}
