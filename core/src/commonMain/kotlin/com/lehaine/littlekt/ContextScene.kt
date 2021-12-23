package com.lehaine.littlekt

import kotlin.time.Duration

/**
 * Created by Colt Daily 12/23/21
 */
open class ContextScene(context: Context) : ContextListener(context) {
    var loading = true
    var prepared = false

    init {
        context.vfs.launch {
            loadAssets()
            loading = false
        }
    }

    /**
     * Runs in a separate thread. Load any assets here.
     * If an asset needs to be prepared, then prepare it in the [prepare] function.
     */
    open suspend fun loadAssets() {}

    /**
     * Runs on the ui thread. Prepare any asset here.
     */
    open fun prepare() {}

    /**
     * Override [update] instead!! Render will call update when the scene has finished loading and preparing.
     */
    final override fun render(dt: Duration) {
        if (loading) return
        if (!prepared) {
            prepare()
            prepared = true
        }

        update(dt)
    }

    open fun update(dt: Duration) {}
}