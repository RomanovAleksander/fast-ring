package com.oleksandr.fastflow

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

    private fun isShowing(id: Int): Boolean =
        composeRule.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty()

    /** Screens carry the same words as their tabs, so match the tab by its role. */
    private fun tapTab(id: Int) {
        val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        composeRule.onNode(hasText(text(id)) and isTab).performClick()
        composeRule.waitForIdle()
    }

    /** Screen content arrives from a Flow, which outlives waitForIdle. */
    private fun awaitText(id: Int) {
        composeRule.waitUntil(timeoutMillis = 5_000) { isShowing(id) }
    }

    /**
     * Onboarding is only shown until it is completed, and that flag outlives a
     * single test, so this walks through it only when it is actually on screen.
     */
    @Before
    fun startFromHome() {
        if (isShowing(R.string.action_next)) {
            composeRule.onNodeWithText(text(R.string.action_next)).performClick()
            composeRule.onNodeWithText(text(R.string.action_done)).performClick()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun tappingTheTimerTabReturnsHomeFromEveryOtherTab() {
        listOf(R.string.tab_history, R.string.tab_stats, R.string.tab_settings).forEach { tab ->
            tapTab(tab)
            tapTab(R.string.tab_timer)

            // The start button exists only on Home.
            awaitText(R.string.action_start)
            composeRule.onNodeWithText(text(R.string.action_start)).assertIsDisplayed()
        }
    }

    @Test
    fun everyTabIsReachableFromEveryOtherTab() {
        // Assertions use wording unique to each screen, never the tab labels.
        tapTab(R.string.tab_history)
        awaitText(R.string.history_empty)

        tapTab(R.string.tab_stats)
        awaitText(R.string.stats_tab_overview)

        tapTab(R.string.tab_settings)
        awaitText(R.string.settings_section_appearance)

        tapTab(R.string.tab_history)
        awaitText(R.string.history_empty)
    }

    @Test
    fun tappingTheAlreadySelectedTabKeepsItOnScreen() {
        tapTab(R.string.tab_settings)
        awaitText(R.string.settings_section_appearance)

        tapTab(R.string.tab_settings)
        awaitText(R.string.settings_section_appearance)
    }
}
