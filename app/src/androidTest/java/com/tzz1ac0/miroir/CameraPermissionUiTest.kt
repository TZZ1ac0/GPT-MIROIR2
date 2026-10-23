package com.tzz1ac0.miroir

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraPermissionUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun missingCameraPermissionShowsExplanationAndRequestButton() {
        setCameraPermission(granted = false)
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            "L’autorisation caméra est nécessaire pour démarrer la capture."
        ).assertExists()
        composeTestRule.onNodeWithText("Autoriser la caméra").assertExists()
    }

    @Test
    fun deniedCameraPermissionKeepsExplanationVisible() {
        // A revoked permission represents the post-denial state without depending on
        // system permission-dialog labels, which vary by Android image and locale.
        setCameraPermission(granted = false)
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            "L’autorisation caméra est nécessaire pour démarrer la capture."
        ).assertExists()
        composeTestRule.onNodeWithText("Autoriser la caméra").assertExists()
    }

    @Test
    fun grantedCameraPermissionDismissesExplanation() {
        setCameraPermission(granted = true)
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            "L’autorisation caméra est nécessaire pour démarrer la capture."
        ).assertDoesNotExist()
        composeTestRule.onNodeWithText("Autoriser la caméra").assertDoesNotExist()
    }

    private fun setCameraPermission(granted: Boolean) {
        val command = if (granted) {
            "pm grant com.tzz1ac0.miroir android.permission.CAMERA"
        } else {
            "pm revoke com.tzz1ac0.miroir android.permission.CAMERA"
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
            .close()
    }
}
