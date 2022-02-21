package com.lehaine.littlekt

import com.lehaine.littlekt.util.Clipboard
import com.lehaine.littlekt.util.internal.jsObject
import kotlinx.browser.window

/**
 * @author Colton Daily
 * @date 2/21/2022
 */
class JsClipboard : Clipboard {
    private val clipboard get() = window.navigator.clipboard
    private var content: String = ""
    private val isFireFox = window.navigator.userAgent.lowercase().indexOf("firefox") > -1
    private var requestWritePermissions = false
    private var hasWritePermissions = true

    override val hasContents: Boolean
        get() = content.isNotEmpty()

    override var contents: String?
        get() = content
        set(value) {
            if (requestWritePermissions || isFireFox) {
                if (hasWritePermissions) {
                    clipboard.writeText(value ?: "")
                }
            } else {
                window.navigator.asDynamic().permissions.query(jsObject { name = "clipboard-write" }).then { result ->
                    if (result.state == "granted" || result.state == "prompt") {
                        content = value ?: ""
                        clipboard.writeText(value ?: "")
                        hasWritePermissions = true
                    }
                    if (result.state == "denied") {
                        hasWritePermissions = false
                    }
                }
                requestWritePermissions = true
            }
        }
}