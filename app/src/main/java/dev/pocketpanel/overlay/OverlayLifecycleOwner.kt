package dev.pocketpanel.overlay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Minimal lifecycle and saved-state owner for a `ComposeView` hosted by a
 * service.
 *
 * The overlay has no Activity, so the service supplies the owners that
 * Compose reads from the view tree. Compose requires a saved-state
 * registry even when the content has nothing to restore, so an empty
 * registry is attached here. View-model storage is not used and is left
 * unset.
 */
class OverlayLifecycleOwner : SavedStateRegistryOwner {

    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    init {
        // Nothing is ever restored, but Compose requires an attached
        // registry before the first composition.
        savedStateController.performAttach()
        savedStateController.performRestore(null)
    }

    fun start() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
