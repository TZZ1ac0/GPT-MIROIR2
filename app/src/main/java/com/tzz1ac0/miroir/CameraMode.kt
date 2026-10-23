package com.tzz1ac0.miroir

/**
 * Camera preview modes exposed by the application menu.
 *
 * Kept independent from Android/Compose so the selection rules can be unit-tested on the JVM.
 */
enum class CameraMode(val title: String) {
    CAMERAX("CameraX"),
    ROOT_ENCODER("RootEncoder");

    companion object {
        val DEFAULT = CAMERAX
    }

    /** The active mode is shown as disabled in the mode-selection menu. */
    fun isSelectableWhen(activeMode: CameraMode): Boolean = this != activeMode
}
