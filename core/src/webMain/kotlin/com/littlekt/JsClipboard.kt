package com.littlekt

import com.littlekt.util.Clipboard
import kotlinx.browser.window

/**
 * @author Colton Daily
 * @date 2/21/2022
 */
class JsClipboard : Clipboard {

    private var content: String = ""
    override val hasContents: Boolean
        get() = content.isNotEmpty()

    override var contents: String?
        get() = content
        set(value) {
            content = value ?: ""
            window.navigator.clipboard.writeText(content)
        }
}