package com.lehaine.littlekt.io

import com.lehaine.littlekt.Application

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
interface Asset {

    /**
     * Load the asset in the system.
     * ie:
     * - load a model into Open GL
     * - load a sound into the Audio Context
     */
    fun load(application: Application)

    /**
     * When the asset is finally loaded, the callback is invoked.
     */
    fun onLoad(callback: (Asset) -> Unit) = Unit
}