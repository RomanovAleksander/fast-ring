package com.oleksandr.fastflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
 */
@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun theAppLaunchesAndShowsOnboarding() {
        val title = composeRule.activity.getString(R.string.onboarding_1_title)
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun onboardingAdvancesToTheNotificationsPage() {
        val next = composeRule.activity.getString(R.string.action_next)
        composeRule.onNodeWithText(next).performClick()

        val notificationsTitle = composeRule.activity.getString(R.string.onboarding_2_title)
        composeRule.onNodeWithText(notificationsTitle).assertIsDisplayed()
    }

    @Test
    fun skippingOnboardingReachesTheTimerTab() {
        // "Далі" then "Готово" leaves onboarding without granting anything.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_next))
            .performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_done))
            .performClick()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tab_timer))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_start))
            .assertIsDisplayed()
    }
}
