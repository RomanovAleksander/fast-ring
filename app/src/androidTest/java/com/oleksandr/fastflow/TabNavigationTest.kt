package com.oleksandr.fastflow

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
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
 * Regression guard for the tab bar.
 *
 * Returning to the timer tab by tapping it did nothing — only system back
 * worked — because the timer is the navigation graph's start destination, so
 * `navigate()` to it degenerates into a no-op and only a pop moves the stack.
 */
@RunWith(AndroidJUnit4::class)
class TabNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun isShowing(id: Int): Boolean =
        composeRule.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty()

    /** Screens carry the same words as their tabs, so match the tab by its role. */
    private fun tapTab(id: Int) {
        composeRule.onAllNodes(hasText(text(id)) and isTab).onFirst().performClick()
        composeRule.waitForIdle()
    }

    /**
     * The bar's selected flag is derived from the navigation back stack, which
     * makes it the most direct evidence that a tap actually navigated. Screen
     * content is not: a `LazyColumn` keeps only its visible rows in the
     * semantics tree, so anything below the fold can never be found.
     */
    private fun awaitSelectedTab(id: Int) = await("tab ${text(id)} selected") {
        composeRule.onAllNodes(hasText(text(id)) and isTab and isSelected())
            .fetchSemanticsNodes().isNotEmpty()
    }

    /** Screen content arrives from a Flow, which outlives waitForIdle. */
    private fun awaitText(id: Int) = await("text ${text(id)}") { isShowing(id) }

    /** Rethrows a timeout with the semantics tree, which says what *is* on screen. */
    private fun await(what: String, condition: () -> Boolean) {
        try {
            composeRule.waitUntil(timeoutMillis = 5_000, condition = condition)
        } catch (timeout: Throwable) {
            throw AssertionError(
                "Never saw $what. Semantics tree:\n" +
                    composeRule.onRoot().printToString(maxDepth = 6),
                timeout,
            )
        }
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
        awaitSelectedTab(R.string.tab_timer)
    }

    @Test
    fun tappingTheTimerTabReturnsHomeFromEveryOtherTab() {
        listOf(R.string.tab_history, R.string.tab_stats, R.string.tab_settings).forEach { tab ->
            tapTab(tab)
            awaitSelectedTab(tab)

            tapTab(R.string.tab_timer)
            awaitSelectedTab(R.string.tab_timer)

            // The start button exists only on Home, and sits above the fold.
            awaitText(R.string.action_start)
            composeRule.onNodeWithText(text(R.string.action_start)).assertIsDisplayed()
        }
    }

    @Test
    fun everyTabIsReachableFromEveryOtherTab() {
        tapTab(R.string.tab_history)
        awaitSelectedTab(R.string.tab_history)
        awaitText(R.string.history_empty)

        tapTab(R.string.tab_stats)
        awaitSelectedTab(R.string.tab_stats)
        awaitText(R.string.stats_tab_overview)

        tapTab(R.string.tab_settings)
        awaitSelectedTab(R.string.tab_settings)

        tapTab(R.string.tab_history)
        awaitSelectedTab(R.string.tab_history)
        awaitText(R.string.history_empty)
    }

    @Test
    fun tappingTheAlreadySelectedTabKeepsItOnScreen() {
        tapTab(R.string.tab_settings)
        awaitSelectedTab(R.string.tab_settings)

        tapTab(R.string.tab_settings)
        awaitSelectedTab(R.string.tab_settings)
    }
}
