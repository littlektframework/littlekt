package com.lehaine.littlekt

/**
 * Signals that the annotated annotation class is a marker of an experimental LittleKt API.
 */
@RequiresOptIn("This is marked as experimental and is not currently stable.", RequiresOptIn.Level.WARNING)
annotation class Experimental