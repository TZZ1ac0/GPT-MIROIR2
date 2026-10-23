package com.tzz1ac0.miroir

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class CameraPermissionUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun missingCameraPermissionShowsExplanationAndRequestButton() {
        revokeCameraPermissionAndRecreateActivity()

        composeTestRule.onNodeWithText(
            "L’autorisation caméra est nécessaire pour démarrer la capture."
        ).assertExists()
        composeTestRule.onNodeWithText("Autoriser la caméra").assertExists()
    }

    @Test
    fun denyingCameraPermissionKeepsExplanationVisible() {
        revokeCameraPermissionAndRecreateActivity()

        composeTestRule.onNodeWithText("Autoriser la caméra").performClick()
        clickPermissionDialogButton(".*permission_deny_button")

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            "L’autorisation caméra est nécessaire pour démarrer la capture."
        ).assertExists()
        composeTestRule.onNodeWithText("Autoriser la caméra").assertExists()
    }

    @Test
    fun grantingCameraPermissionDismissesExplanation() {
        revokeCameraPermissionAndRecreateActivity()

        composeTestRule.onNodeWithText("Autoriser la caméra").performClick()
        clickPermissionDialogButton(
            ".*permission_allow_foreground_only_button|.*permission_allow_button|.*permission_allow_one_time_button"
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            "L’autorisation caméra est nécessaire pour démarrer la capture."
        ).assertDoesNotExist()
        composeTestRule.onNodeWithText("Autoriser la caméra").assertDoesNotExist()
    }

    private fun revokeCameraPermissionAndRecreateActivity() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm revoke com.tzz1ac0.miroir android.permission.CAMERA")
            .close()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    private fun clickPermissionDialogButton(resourceIdPattern: String) {
        val button = device.wait(
            Until.findObject(By.res(Pattern.compile(resourceIdPattern))),
            5_000
        )
        checkNotNull(button) { "Could not find permission dialog button matching $resourceIdPattern" }
            .click()
    }
}
