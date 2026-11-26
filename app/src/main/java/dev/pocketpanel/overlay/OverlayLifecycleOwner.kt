package dev.pocketpanel.overlay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Minimal lifecycle owner for a `ComposeView` hosted by a service.
 *
 * The overlay has no Activity, so the service supplies the lifecycle that
 * Compose needs for composition and disposal. State restoration and
 * view-model storage are not used by the overlay.
 */
class OverlayLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    fun start() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
