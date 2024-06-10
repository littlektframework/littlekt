package com.littlekt.util

/**
 * @author Colton Daily
 * @date 2/21/2022
 */
interface Clipboard {
    /** Checks if the clipboard has contents. */
    val hasContents: Boolean

    /** The clipboard contents. */
    var contents: String?
}
