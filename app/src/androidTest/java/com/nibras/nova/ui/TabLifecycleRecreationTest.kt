package com.nibras.nova.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nibras.nova.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabLifecycleRecreationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun activitySurvivesConfigurationRecreationWithoutCrash() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithContentDescription("Nibras Nova Browser Logo").assertExists()
    }
}
