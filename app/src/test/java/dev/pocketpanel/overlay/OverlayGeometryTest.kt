package dev.pocketpanel.overlay

import dev.pocketpanel.settings.HandleEdge
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayGeometryTest {

    @Test
    fun placementPinsTheLeftEdgeToZero() {
        val placement = OverlayGeometry.placementFor(
            edge = HandleEdge.LEFT,
            verticalFraction = 0.25f,
            windowWidth = 100,
            windowHeight = 200,
            screenWidth = 1000,
            screenHeight = 2000,
        )

        assertEquals(0, placement.x)
        assertEquals(450, placement.y)
        assertEquals(HandleEdge.LEFT, placement.edge)
        assertEquals(0.25f, placement.verticalFraction, 0.0001f)
    }

    @Test
    fun placementPinsTheRightEdgeToTheRemainingWidth() {
        val placement = OverlayGeometry.placementFor(
            edge = HandleEdge.RIGHT,
            verticalFraction = 0.5f,
            windowWidth = 100,
            windowHeight = 200,
            screenWidth = 1000,
            screenHeight = 2000,
        )

        assertEquals(900, placement.x)
        assertEquals(900, placement.y)
        assertEquals(HandleEdge.RIGHT, placement.edge)
    }

    @Test
    fun placementClampsTheVerticalFraction() {
        val top = OverlayGeometry.placementFor(
            edge = HandleEdge.LEFT,
            verticalFraction = -1f,
            windowWidth = 100,
            windowHeight = 200,
            screenWidth = 1000,
            screenHeight = 2000,
        )
        val bottom = OverlayGeometry.placementFor(
            edge = HandleEdge.LEFT,
            verticalFraction = 2f,
            windowWidth = 100,
            windowHeight = 200,
            screenWidth = 1000,
            screenHeight = 2000,
        )

        assertEquals(0, top.y)
        assertEquals(0f, top.verticalFraction, 0.0001f)
        assertEquals(1800, bottom.y)
        assertEquals(1f, bottom.verticalFraction, 0.0001f)
    }

    @Test
    fun placementHandlesAWindowLargerThanTheScreen() {
        val placement = OverlayGeometry.placementFor(
            edge = HandleEdge.RIGHT,
            verticalFraction = 0.5f,
            windowWidth = 1200,
            windowHeight = 2200,
            screenWidth = 1000,
            screenHeight = 2000,
        )

        assertEquals(0, placement.x)
        assertEquals(0, placement.y)
    }

    @Test
    fun snapChoosesTheNearestHorizontalEdge() {
        val left = OverlayGeometry.snapToNearestEdge(
            x = 200,
            y = 300,
            windowWidth = 100,
            windowHeight = 200,
            screenWidth = 1000,
            screenHeight = 2000,
        )
        val right = OverlayGeometry.snapToNearestEdge(
            x = 700,
            y = 300,
            windowWidth = 100,
            windowHeight = 200,
            screenWidth = 1000,
            screenHeight = 2000,
        )

        assertEquals(HandleEdge.LEFT, left.edge)
        assertEquals(0, left.x)
        assertEquals(HandleEdge.RIGHT, right.edge)
        assertEquals(900, right.x)
    }

    @Test
    fun snapClampsThePositionAndDerivesTheFraction() {
        val placement = OverlayGeometry.snapToNearestEdge(
            x = 5000,
            y = 5000,
            windowWidth = 100,
            windowHeight = 200,
            screenWidth = 1000,
            screenHeight = 2000,
        )

        assertEquals(HandleEdge.RIGHT, placement.edge)
        assertEquals(900, placement.x)
        assertEquals(1800, placement.y)
        assertEquals(1f, placement.verticalFraction, 0.0001f)
    }

    @Test
    fun snapUsesZeroFractionWhenThereIsNoVerticalTravel() {
        val placement = OverlayGeometry.snapToNearestEdge(
            x = 0,
            y = 100,
            windowWidth = 100,
            windowHeight = 200,
            screenWidth = 1000,
            screenHeight = 200,
        )

        assertEquals(0, placement.y)
        assertEquals(0f, placement.verticalFraction, 0.0001f)
    }
}
