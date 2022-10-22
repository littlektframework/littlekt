package com.lehaine.littlekt

/**
 * Signals that the annotated annotation class is a marker of an experimental LittleKt API.
 * This means that the API marked is most likely unstable and subject to changes without notice.
 */
@RequiresOptIn("This is marked as experimental and is not currently stable. Changes to this API may change without notice!", RequiresOptIn.Level.WARNING)
annotation class Experimental