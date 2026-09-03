package com.oleksandr.fastflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for the tab bar.
 *
 * Returning to the timer tab by tapping it did nothing — only system back
 * worked — because the timer is the navigation graph's start destination and
 * popping only *down to* it made the navigate a no-op.
 */
@RunWith(AndroidJUnit4::class)
class TabNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun tapTab(id: Int) {
        composeRule.onNodeWithText(text(id)).performClick()
        composeRule.waitForIdle()
    }

    @Before
    fun skipOnboarding() {
        composeRule.onNodeWithText(text(R.string.action_next)).performClick()
        composeRule.onNodeWithText(text(R.string.action_done)).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun tappingTheTimerTabReturnsHomeFromEveryOtherTab() {
        listOf(R.string.tab_history, R.string.tab_stats, R.string.tab_settings).forEach { tab ->
            tapTab(tab)
            tapTab(R.string.tab_timer)

            // The start button only exists on Home.
            composeRule.onNodeWithText(text(R.string.action_start)).assertIsDisplayed()
        }
    }

    @Test
    fun everyTabIsReachableFromEveryOtherTab() {
        tapTab(R.string.tab_history)
        composeRule.onNodeWithText(text(R.string.screen_history_title)).assertIsDisplayed()

        tapTab(R.string.tab_stats)
        composeRule.onNodeWithText(text(R.string.stats_tab_overview)).assertIsDisplayed()

        tapTab(R.string.tab_settings)
        composeRule.onNodeWithText(text(R.string.settings_section_appearance)).assertIsDisplayed()

        tapTab(R.string.tab_history)
        composeRule.onNodeWithText(text(R.string.screen_history_title)).assertIsDisplayed()
    }

    @Test
    fun tappingTheAlreadySelectedTabKeepsItOnScreen() {
        tapTab(R.string.tab_settings)
        tapTab(R.string.tab_settings)
        composeRule.onNodeWithText(text(R.string.settings_section_appearance)).assertIsDisplayed()
    }
}
