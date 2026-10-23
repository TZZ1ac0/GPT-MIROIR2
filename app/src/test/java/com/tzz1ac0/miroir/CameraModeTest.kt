package com.tzz1ac0.miroir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraModeTest {
    @Test
    fun cameraXIsTheDefaultMode() {
        assertEquals(CameraMode.CAMERAX, CameraMode.DEFAULT)
    }

    @Test
    fun onlyTheInactiveModeIsSelectableWhenCameraXIsActive() {
        assertFalse(CameraMode.CAMERAX.isSelectableWhen(CameraMode.CAMERAX))
        assertTrue(CameraMode.ROOT_ENCODER.isSelectableWhen(CameraMode.CAMERAX))
    }

    @Test
    fun onlyTheInactiveModeIsSelectableWhenRootEncoderIsActive() {
        assertTrue(CameraMode.CAMERAX.isSelectableWhen(CameraMode.ROOT_ENCODER))
        assertFalse(CameraMode.ROOT_ENCODER.isSelectableWhen(CameraMode.ROOT_ENCODER))
    }

    @Test
    fun eachModeHasTheExpectedTitle() {
        assertEquals("CameraX", CameraMode.CAMERAX.title)
        assertEquals("RootEncoder", CameraMode.ROOT_ENCODER.title)
    }
}
