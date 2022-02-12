package com.lehaine.littlekt.async

import kotlinx.coroutines.CoroutineScope

/**
 * Returns true if the coroutine was launched from the rendering thread dispatcher.
 */
actual fun CoroutineScope.isOnRenderingThread(): Boolean {
    TODO("Not yet implemented")
}