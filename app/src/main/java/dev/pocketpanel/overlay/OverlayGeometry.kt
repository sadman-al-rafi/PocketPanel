package dev.pocketpanel.overlay

import dev.pocketpanel.settings.HandleEdge

/** Resolved overlay window position in screen pixels. */
data class OverlayPlacement(
    val x: Int,
    val y: Int,
    val edge: HandleEdge,
    val verticalFraction: Float,
)

/**
 * Pure positioning math for the floating handle.
 *
 * [screenWidth] and [screenHeight] are the usable window area (the display
 * minus system bars). The result is relative to that area, so the caller
 * applies [OverlayPlacement.x] and [OverlayPlacement.y] directly as the
 * window offsets with `Gravity.TOP or Gravity.START`.
 */
object OverlayGeometry {

    fun placementFor(
        edge: HandleEdge,
        verticalFraction: Float,
        windowWidth: Int,
        windowHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): OverlayPlacement {
        val fraction = verticalFraction.coerceIn(0f, 1f)
        val maxX = (screenWidth - windowWidth).coerceAtLeast(0)
        val maxY = (screenHeight - windowHeight).coerceAtLeast(0)
        val x = if (edge == HandleEdge.LEFT) 0 else maxX
        val y = (fraction * maxY).toInt().coerceIn(0, maxY)
        return OverlayPlacement(x = x, y = y, edge = edge, verticalFraction = fraction)
    }

    /**
     * Clamps a dragged position into the screen and snaps it to the
     * nearest horizontal edge. The vertical fraction is derived from the
     * final position so it can be persisted.
     */
    fun snapToNearestEdge(
        x: Int,
        y: Int,
        windowWidth: Int,
        windowHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): OverlayPlacement {
        val maxX = (screenWidth - windowWidth).coerceAtLeast(0)
        val maxY = (screenHeight - windowHeight).coerceAtLeast(0)
        val clampedX = x.coerceIn(0, maxX)
        val clampedY = y.coerceIn(0, maxY)
        val edge = if (clampedX + windowWidth / 2 < screenWidth / 2) {
            HandleEdge.LEFT
        } else {
            HandleEdge.RIGHT
        }
        val fraction = if (maxY == 0) 0f else clampedY.toFloat() / maxY
        return OverlayPlacement(
            x = if (edge == HandleEdge.LEFT) 0 else maxX,
            y = clampedY,
            edge = edge,
            verticalFraction = fraction.coerceIn(0f, 1f),
        )
    }
}
