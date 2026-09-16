package com.example.data.model

import android.net.Uri

enum class BlendMode(val displayName: String, val subtitle: String) {
    STUDIO_HARMONY("Studio Harmony", "Smooth seam cross-fade & tonal balance"),
    SEAMLESS_FEATHER("Soft Dissolve", "Pure feathered gradient cross-fade"),
    DREAMY_VIGNETTE("Dreamy Glow", "Soft ambient blend with subtle vignette"),
    EDITORIAL_FILM("Editorial Film", "Cinematic contrast & unified warm tones"),
    CLEAN_GRID("Classic Cut", "Modern crisp edge-to-edge layout")
}

enum class CollageLayout(val slotCount: Int, val displayName: String) {
    TWO_SPLIT_VERTICAL(2, "Vertical"),
    TWO_SPLIT_HORIZONTAL(2, "Horizontal"),
    THREE_GRID(3, "3-Grid"),
    THREE_ROWS(3, "3-Rows"),
    THREE_COLUMNS(3, "3-Cols"),
    FOUR_GRID(4, "4-Grid"),
    FOUR_ROWS(4, "4-Rows"),
    FOUR_COLUMNS(4, "4-Cols"),
    FOUR_FEATURED(4, "Featured")
}

sealed interface SlotState {
    object Empty : SlotState
    object Locked : SlotState
    object Loading : SlotState
    object CameraActive : SlotState
    data class Photo(val uri: Uri) : SlotState
}

fun createInitialSlots(slotCount: Int): List<SlotState> {
    return List(slotCount) { SlotState.CameraActive }
}

data class CollageState(
    val layout: CollageLayout = CollageLayout.FOUR_GRID,
    val slots: List<SlotState> = createInitialSlots(CollageLayout.FOUR_GRID.slotCount),
    val isCircularStyle: Boolean = false,
    val selectedGradientIndex: Int = 0, // 0 means default/no replacement, 1+ are various custom gradients
    val isBackgroundRemoved: Boolean = false,
    val customBackgroundUri: Uri? = null,
    val blendMode: BlendMode = BlendMode.STUDIO_HARMONY,
    val blendFeatherPx: Int = 48
)
