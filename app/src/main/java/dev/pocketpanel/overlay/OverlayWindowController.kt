package dev.pocketpanel.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.pocketpanel.R
import dev.pocketpanel.settings.HandleEdge
import dev.pocketpanel.settings.UserPreferences
import dev.pocketpanel.ui.theme.PocketPanelTheme
import dev.pocketpanel.vpn.VpnConnectionState
import kotlin.math.roundToInt

/**
 * Owns the single overlay window and its Compose content.
 *
 * The window is a fixed-size [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * view. When collapsed it is the size of the handle; when expanded it also
 * contains the panel. The window is always anchored to the left or right
 * edge, and the vertical fraction is persisted by [onPositionChanged].
 *
 * Coordinates are relative to the usable window area (the display minus
 * system bars), because the window relies on the platform's default
 * `fitInsetsTypes` behavior instead of drawing edge to edge.
 */
class OverlayWindowController(
    context: Context,
    lifecycleOwner: SavedStateRegistryOwner,
    private val onPositionChanged: (HandleEdge, Float) -> Unit,
    private val onVpnToggle: () -> Unit,
) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)!!
    private val composeView = ComposeView(
        ContextThemeWrapper(appContext, R.style.Theme_PocketPanel),
    ).apply {
        // An overlay window has no Activity, so the service supplies the
        // lifecycle and saved-state owners that Compose requires. The
        // overlay does not use view models.
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setContent {
            val currentPreferences = preferences
            val currentExpanded = expanded
            PocketPanelTheme(
                darkTheme = currentPreferences.themeMode
                    .resolveDarkTheme(isSystemInDarkTheme()),
                dynamicColor = currentPreferences.dynamicColor,
            ) {
                OverlayContent(
                    preferences = currentPreferences,
                    expanded = currentExpanded,
                    onToggleExpanded = ::toggleExpanded,
                    onCollapse = { applyExpanded(false) },
                    onDragStart = ::onDragStart,
                    onDrag = ::onDrag,
                    onDragEnd = ::onDragEnd,
                    screenOrigin = ::currentScreenOrigin,
                    vpnState = vpnState,
                    hasVpnProfile = hasVpnProfile,
                    onVpnToggle = onVpnToggle,
                    monitoring = monitoring,
                )
            }
        }
    }

    private var preferences by mutableStateOf(UserPreferences())
    private var expanded by mutableStateOf(false)
    private var vpnState by mutableStateOf<VpnConnectionState>(VpnConnectionState.Disconnected)
    private var hasVpnProfile by mutableStateOf(false)
    private var monitoring by mutableStateOf<OverlayMonitoring?>(null)

    private var layoutParams: WindowManager.LayoutParams? = null
    private var attached = false
    private var dragging = false

    private var edge = HandleEdge.RIGHT
    private var verticalFraction = 0.5f

    private var availableWidth = 1
    private var availableHeight = 1

    private var dragX = 0f
    private var dragY = 0f

    /** Adds the overlay. Returns false when the platform rejects the window. */
    fun show(initialPreferences: UserPreferences): Boolean {
        if (attached) {
            update(initialPreferences)
            return true
        }

        preferences = initialPreferences
        edge = initialPreferences.handleEdge
        verticalFraction = initialPreferences.handleVerticalFraction.coerceIn(0f, 1f)
        expanded = false
        refreshAvailableSize()

        val (width, height) = sizeFor(expanded = false)
        val placement = OverlayGeometry.placementFor(
            edge = edge,
            verticalFraction = verticalFraction,
            windowWidth = width,
            windowHeight = height,
            screenWidth = availableWidth,
            screenHeight = availableHeight,
        )
        val params = createLayoutParams(width, height).apply {
            x = placement.x
            y = placement.y
        }

        return try {
            windowManager.addView(composeView, params)
            layoutParams = params
            attached = true
            // Re-apply after the first layout so that a display or inset
            // change during startup cannot leave the handle off-screen.
            composeView.post { if (attached) applyPlacement(expanded) }
            true
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to add the overlay window", error)
            layoutParams = null
            attached = false
            false
        }
    }

    /** Applies a new settings snapshot without touching the window position. */
    fun update(newPreferences: UserPreferences) {
        val sizeChanged = preferences.handleSize != newPreferences.handleSize
        val edgeChanged = preferences.handleEdge != newPreferences.handleEdge
        preferences = newPreferences
        if (sizeChanged || edgeChanged) {
            edge = newPreferences.handleEdge
            if (attached) applyPlacement(expanded)
        }
    }

    /** Updates the VPN row from the shared repository state. */
    fun updateVpnState(state: VpnConnectionState, profileAvailable: Boolean) {
        vpnState = state
        hasVpnProfile = profileAvailable
    }

    /** Updates the 2x2 monitoring grid; null hides it. */
    fun updateMonitoring(sample: OverlayMonitoring?) {
        monitoring = sample
    }

    /** Re-anchors the window after a rotation or other configuration change. */
    fun onConfigurationChanged() {
        if (attached) applyPlacement(expanded)
    }

    /** Removes the window synchronously. Safe to call more than once. */
    fun hide() {
        if (!attached) return
        attached = false
        layoutParams = null
        runCatching { windowManager.removeViewImmediate(composeView) }
            .onFailure { Log.w(TAG, "Unable to remove the overlay window", it) }
    }

    private fun toggleExpanded() {
        applyExpanded(!expanded)
    }

    /**
     * Named `applyExpanded` rather than `setExpanded` because the
     * `expanded` property already generates a `setExpanded` JVM setter.
     */
    private fun applyExpanded(newExpanded: Boolean) {
        if (expanded == newExpanded) return
        expanded = newExpanded
        if (attached) applyPlacement(newExpanded)
    }

    private fun onDragStart() {
        if (expanded || !attached) return
        refreshAvailableSize()
        dragging = true
        dragX = (layoutParams?.x ?: 0).toFloat()
        dragY = (layoutParams?.y ?: 0).toFloat()
    }

    /**
     * Uses the window offsets we just wrote instead of querying the view
     * location again. `getLocationOnScreen` can lag one frame behind an
     * `updateViewLayout` call, which makes dragging feel stepwise.
     */
    private fun currentScreenOrigin(): Offset {
        val params = layoutParams
        return Offset(
            (params?.x ?: 0).toFloat(),
            (params?.y ?: 0).toFloat(),
        )
    }

    private fun onDrag(deltaX: Float, deltaY: Float) {
        if (!dragging || expanded) return
        val params = layoutParams ?: return
        val maxX = (availableWidth - params.width).coerceAtLeast(0).toFloat()
        val maxY = (availableHeight - params.height).coerceAtLeast(0).toFloat()

        dragX = (dragX + deltaX).coerceIn(0f, maxX)
        dragY = (dragY + deltaY).coerceIn(0f, maxY)

        params.x = dragX.roundToInt()
        params.y = dragY.roundToInt()
        windowManager.updateViewLayout(composeView, params)
    }

    private fun onDragEnd() {
        if (!dragging) return
        dragging = false
        val params = layoutParams ?: return
        refreshAvailableSize()

        val placement = OverlayGeometry.snapToNearestEdge(
            x = params.x,
            y = params.y,
            windowWidth = params.width,
            windowHeight = params.height,
            screenWidth = availableWidth,
            screenHeight = availableHeight,
        )
        edge = placement.edge
        verticalFraction = placement.verticalFraction
        preferences = preferences.copy(
            handleEdge = placement.edge,
            handleVerticalFraction = placement.verticalFraction,
        )
        params.x = placement.x
        params.y = placement.y
        windowManager.updateViewLayout(composeView, params)
        onPositionChanged(placement.edge, placement.verticalFraction)
    }

    private fun applyPlacement(isExpanded: Boolean) {
        val params = layoutParams ?: return
        refreshAvailableSize()

        val (width, height) = sizeFor(isExpanded)
        val maxX = (availableWidth - width).coerceAtLeast(0)
        val maxY = (availableHeight - height).coerceAtLeast(0)
        val x = if (edge == HandleEdge.LEFT) 0 else maxX
        val y = if (isExpanded) {
            // Keep the handle's centre where it rested while collapsed, so
            // opening the panel does not make the handle jump vertically.
            val handleHeight = sizeFor(expanded = false).second
            val collapsedY = (verticalFraction *
                (availableHeight - handleHeight).coerceAtLeast(0)).roundToInt()
            (collapsedY + handleHeight / 2 - height / 2).coerceIn(0, maxY)
        } else {
            (verticalFraction * maxY).roundToInt().coerceIn(0, maxY)
        }

        params.width = width
        params.height = height
        params.x = x
        params.y = y
        windowManager.updateViewLayout(composeView, params)
    }

    private fun createLayoutParams(width: Int, height: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "PocketPanel overlay"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
        }

    private fun sizeFor(expanded: Boolean): Pair<Int, Int> {
        val density = appContext.resources.displayMetrics.density
        val size = OverlayDimensions.forSize(preferences.handleSize)
        return if (expanded) {
            (size.expandedWidth.value * density).roundToInt() to
                (size.expandedHeight.value * density).roundToInt()
        } else {
            (size.handleWidth.value * density).roundToInt() to
                (size.handleHeight.value * density).roundToInt()
        }
    }

    @Suppress("DEPRECATION")
    private fun refreshAvailableSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            availableWidth = (metrics.bounds.width() - insets.left - insets.right)
                .coerceAtLeast(1)
            availableHeight = (metrics.bounds.height() - insets.top - insets.bottom)
                .coerceAtLeast(1)
        } else {
            val size = Point()
            windowManager.defaultDisplay.getSize(size)
            availableWidth = size.x.coerceAtLeast(1)
            availableHeight = size.y.coerceAtLeast(1)
        }
    }

    private companion object {
        const val TAG = "OverlayWindow"
    }
}
