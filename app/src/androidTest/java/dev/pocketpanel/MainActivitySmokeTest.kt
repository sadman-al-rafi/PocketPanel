package dev.pocketpanel

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pocketpanel.ui.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAndShowsMainScreen() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.app_name))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.overlay))
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreenShowsAppearanceOptions() {
        openSettings()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.appearance))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.THEME_SYSTEM).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.THEME_LIGHT).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.THEME_DARK).assertIsDisplayed()
    }

    @Test
    fun selectingDarkPersistsAfterLeavingSettings() {
        openSettings()
        composeRule.onNodeWithTag(TestTags.THEME_DARK).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.THEME_DARK)
                .fetchSemanticsNodes()
                .any { node ->
                    node.config.getOrNull(SemanticsProperties.Selected) == true
                }
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back))
            .performClick()

        // The appearance summary is no longer on the home screen; verify
        // persistence by reopening settings instead.
        openSettings()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.THEME_DARK)
                .fetchSemanticsNodes()
                .any { node ->
                    node.config.getOrNull(SemanticsProperties.Selected) == true
                }
        }
    }

    private fun openSettings() {
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.settings))
            .performClick()
    }
}
