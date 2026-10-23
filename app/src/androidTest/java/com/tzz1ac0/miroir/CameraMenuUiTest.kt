package com.tzz1ac0.miroir

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraMenuUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun cameraXIsShownAsTheDefaultModeAndCanSwitchToRootEncoder() {
        composeTestRule.onNodeWithText("Miroir — CameraX").assertExists()

        openMenu()
        composeTestRule.onNodeWithText("CameraX").assertIsNotEnabled()
        composeTestRule.onNodeWithText("RootEncoder").assertIsEnabled().performClick()

        composeTestRule.onNodeWithText("Miroir — RootEncoder").assertExists()
        openMenu()
        composeTestRule.onNodeWithText("RootEncoder").assertIsNotEnabled()
        composeTestRule.onNodeWithText("CameraX").assertIsEnabled().performClick()

        composeTestRule.onNodeWithText("Miroir — CameraX").assertExists()
    }

    @Test
    fun settingsDialogShowsResolutionInformationAndCanBeClosed() {
        openMenu()
        composeTestRule.onNodeWithText("Paramètres…").performClick()

        composeTestRule.onNodeWithText("Paramètres").assertExists()
        composeTestRule.onNodeWithText("Résolution native du capteur").assertExists()
        composeTestRule.onNodeWithText("Flux vidéo d’entrée").assertExists()
        composeTestRule.onNodeWithText("Image affichée").assertExists()
        composeTestRule.onNodeWithText("Surface d’affichage disponible").assertExists()

        composeTestRule.onNodeWithText("Fermer").performClick()
        composeTestRule.onNodeWithText("Paramètres").assertDoesNotExist()
    }

    private fun openMenu() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
    }
}
