package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BlendMode
import com.example.data.model.CollageLayout
import com.example.data.model.CollageState
import com.example.data.model.SlotState
import com.example.domain.segmentation.CollageRenderer
import com.example.domain.segmentation.SubjectSegmenterHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollageViewModel : ViewModel() {

    private val _collageState = MutableStateFlow(CollageState())
    val collageState: StateFlow<CollageState> = _collageState.asStateFlow()

    // History stacks for full Undo/Redo across actions
    private val undoStack = mutableListOf<CollageState>()
    private val redoStack = mutableListOf<CollageState>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Flag for overall AI processing status (e.g. background segmentation)
    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    // Output state for the fully rendered final collage
    private val _finalCollageBitmap = MutableStateFlow<Bitmap?>(null)
    val finalCollageBitmap: StateFlow<Bitmap?> = _finalCollageBitmap.asStateFlow()

    private fun pushToHistory(state: CollageState) {
        // Deep copy of collage state is implicit since it's a data class containing immutable values / Lists
        undoStack.add(state)
        // Keep stack size reasonable (e.g., max 50 actions)
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoStatus()
    }

    private fun updateUndoRedoStatus() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    /**
     * Keeps all available (uncaptured) camera slots active simultaneously:
     * - Any slot that already has a captured picture (SlotState.Photo) is preserved.
     * - All available / uncaptured camera slots stay active simultaneously (SlotState.CameraActive).
     */
    private fun resolveAvailableCameraSlots(
        existingSlots: List<SlotState>,
        slotCount: Int
    ): List<SlotState> {
        return (0 until slotCount).map { i ->
            val slot = existingSlots.getOrNull(i)
            if (slot is SlotState.Photo) {
                slot
            } else {
                SlotState.CameraActive
            }
        }
    }

    /**
     * Shifts the grid layout (2, 3, or 4 slots) and dynamically maps the slot content states,
     * keeping all available camera slots active simultaneously.
     */
    fun selectLayout(newLayout: CollageLayout) {
        val current = _collageState.value
        if (current.layout == newLayout) return

        pushToHistory(current)

        _collageState.update { state ->
            val resolvedSlots = resolveAvailableCameraSlots(state.slots, newLayout.slotCount)
            state.copy(
                layout = newLayout,
                slots = resolvedSlots
            )
        }
    }

    /**
     * Toggles or sets whether the collage layout uses circular style or modern rounded style.
     */
    fun setCircularStyle(isCircular: Boolean) {
        val current = _collageState.value
        if (current.isCircularStyle == isCircular) return

        pushToHistory(current)

        _collageState.update { state ->
            state.copy(isCircularStyle = isCircular)
        }
    }

    /**
     * Assigns a photo Uri to a specific slot index while keeping all remaining camera slots active.
     */
    fun updateSlotWithPhoto(index: Int, uri: Uri) {
        val current = _collageState.value
        if (index !in current.slots.indices) return

        pushToHistory(current)

        _collageState.update { state ->
            val updatedSlots = state.slots.toMutableList()
            while (updatedSlots.size < state.layout.slotCount) {
                updatedSlots.add(SlotState.CameraActive)
            }
            updatedSlots[index] = SlotState.Photo(uri)
            val resolvedSlots = resolveAvailableCameraSlots(updatedSlots, state.layout.slotCount)
            state.copy(slots = resolvedSlots)
        }
    }

    /**
     * Puts a specific slot index in an active CameraX state.
     */
    fun setSlotCameraActive(index: Int) {
        val current = _collageState.value
        if (index !in current.slots.indices) return

        pushToHistory(current)

        _collageState.update { state ->
            val updatedSlots = state.slots.toMutableList()
            updatedSlots[index] = SlotState.CameraActive
            state.copy(slots = updatedSlots)
        }
    }

    /**
     * Puts a specific slot index in a loading state.
     */
    fun setSlotLoading(index: Int) {
        val current = _collageState.value
        if (index !in current.slots.indices) return

        pushToHistory(current)

        _collageState.update { state ->
            val updatedSlots = state.slots.toMutableList()
            updatedSlots[index] = SlotState.Loading
            state.copy(slots = updatedSlots)
        }
    }

    /**
     * Clears slot data to active camera state, keeping all cameras active simultaneously.
     */
    fun clearSlot(index: Int) {
        val current = _collageState.value
        if (index !in current.slots.indices) return

        pushToHistory(current)

        _collageState.update { state ->
            val updatedSlots = state.slots.toMutableList()
            while (updatedSlots.size < state.layout.slotCount) {
                updatedSlots.add(SlotState.CameraActive)
            }
            updatedSlots[index] = SlotState.CameraActive
            val resolvedSlots = resolveAvailableCameraSlots(updatedSlots, state.layout.slotCount)
            state.copy(slots = resolvedSlots)
        }
    }

    /**
     * Sets the AI background replacement index (0 = default/no gradient replacement, 1+ = dynamic gradients)
     */
    fun selectBackgroundGradient(gradientIndex: Int) {
        val current = _collageState.value
        if (current.selectedGradientIndex == gradientIndex) return

        pushToHistory(current)

        _collageState.update { state ->
            state.copy(selectedGradientIndex = gradientIndex)
        }
    }

    /**
     * Toggles whether the final collage should have its original background removed by AI.
     */
    fun toggleBackgroundRemoval() {
        val current = _collageState.value
        pushToHistory(current)

        _collageState.update { state ->
            state.copy(isBackgroundRemoved = !state.isBackgroundRemoved)
        }
    }

    /**
     * Sets overall AI processing state.
     */
    fun setAiProcessing(processing: Boolean) {
        _isAiProcessing.value = processing
    }

    /**
     * Set a custom gallery background image and auto-enable background removal.
     */
    fun setCustomBackgroundUri(uri: Uri?) {
        val current = _collageState.value
        pushToHistory(current)

        _collageState.update { state ->
            state.copy(
                customBackgroundUri = uri,
                isBackgroundRemoved = uri != null // Automatically toggle background removal as requested
            )
        }
    }

    /**
     * Reverts to the previous recorded CollageState.
     */
    fun undo() {
        if (undoStack.isEmpty()) return

        val previousState = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(_collageState.value)

        _collageState.value = previousState
        updateUndoRedoStatus()
    }

    /**
     * Restores the next CollageState in history.
     */
    fun redo() {
        if (redoStack.isEmpty()) return

        val nextState = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(_collageState.value)

        _collageState.value = nextState
        updateUndoRedoStatus()
    }

    /**
     * Clears all slots and resets background states to defaults. Supports undo.
     */
    fun clearAll() {
        val current = _collageState.value
        pushToHistory(current)

        _collageState.update { state ->
            CollageState(
                layout = state.layout,
                slots = resolveAvailableCameraSlots(emptyList(), state.layout.slotCount),
                isCircularStyle = state.isCircularStyle,
                selectedGradientIndex = 0,
                isBackgroundRemoved = false,
                customBackgroundUri = null,
                blendMode = state.blendMode,
                blendFeatherPx = state.blendFeatherPx
            )
        }
    }

    fun setBlendMode(mode: BlendMode, context: Context? = null) {
        _collageState.update { it.copy(blendMode = mode) }
        if (_finalCollageBitmap.value != null && context != null) {
            generateFinalCollage(context)
        }
    }

    fun setBlendFeather(featherPx: Int, context: Context? = null) {
        _collageState.update { it.copy(blendFeatherPx = featherPx) }
        if (_finalCollageBitmap.value != null && context != null) {
            generateFinalCollage(context)
        }
    }

    /**
     * Generates the final merged collage and applies AI background segmentation & replacement
     * if requested. Result is saved to finalCollageBitmap state.
     */
    fun generateFinalCollage(context: Context) {
        viewModelScope.launch {
            _isAiProcessing.value = true
            try {
                // 1. Render base merged collage
                val baseMerged = CollageRenderer.renderCollage(context, _collageState.value)
                
                // 2. Check if background removal / custom or gradient replacement is activated
                val state = _collageState.value
                val finalResult = if (state.customBackgroundUri != null) {
                    val segmenterHelper = SubjectSegmenterHelper(context)
                    val customBgBitmap = CollageRenderer.loadUriToBitmap(context, state.customBackgroundUri)
                    if (customBgBitmap != null) {
                        // Segment foreground subjects (all subjects with transparency)
                        val transparentForeground = segmenterHelper.removeBackground(baseMerged)
                        // Place foreground subjects onto selected gallery picture background
                        segmenterHelper.applyCustomBackgroundReplacement(transparentForeground, customBgBitmap)
                    } else {
                        baseMerged
                    }
                } else if (state.isBackgroundRemoved) {
                    val segmenterHelper = SubjectSegmenterHelper(context)
                    // Segment foreground subjects (all subjects with transparency)
                    val transparentForeground = segmenterHelper.removeBackground(baseMerged)
                    
                    // Place foreground subjects onto selected gradient background
                    segmenterHelper.applyBackgroundReplacement(transparentForeground, state.selectedGradientIndex)
                } else if (state.selectedGradientIndex > 0) {
                    // Even if background removal isn't checked, we can fill the entire background area
                    // with a gradient if there are transparent spaces or as the global canvas fill
                    val segmenterHelper = SubjectSegmenterHelper(context)
                    segmenterHelper.applyBackgroundReplacement(baseMerged, state.selectedGradientIndex)
                } else {
                    baseMerged
                }
                
                _finalCollageBitmap.value = finalResult
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun clearFinalCollage() {
        _finalCollageBitmap.value = null
    }
}
