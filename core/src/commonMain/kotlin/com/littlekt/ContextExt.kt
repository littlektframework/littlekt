package com.littlekt

internal expect var ownedContext: Context

/**
 * Creates a new _postRunnable_ that is invoked one time after the next frame. This is added to the
 * internal `ownedContext`.
 *
 * @return a lambda that can be invoked to remove the callback
 */
fun postRunnable(action: () -> Unit) = ownedContext.postRunnable(action)
