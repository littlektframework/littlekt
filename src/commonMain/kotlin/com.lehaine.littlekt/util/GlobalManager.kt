package com.lehaine.littlekt.util

/**
 * @author Colton Daily
 * @date 9/29/2021
 */
open class GlobalManager {

    private var _enabled = false

    /**
     * Enables/disables this [GlobalManager]
     *  Changes in state result to [onEnabled] / [onDisabled] being called.
     *  @return `true` if [GlobalManager] is enabled; otherwise, `false`.
     */
    var enabled
        get() = _enabled
        set(value) {
            if (_enabled != value) {
                _enabled = value
                if (_enabled) {
                    onEnabled()
                } else {
                    onDisabled()
                }
            }
        }


    /**
     * Called when this [GlobalManager] is enabled.
     */
    open fun onEnabled() {}

    /**
     * Called when this [GlobalManager] is disabled.
     */
    open fun onDisabled() {}

    /**
     * Called each from before [Scene.update]
     */
    open fun update() {}
}