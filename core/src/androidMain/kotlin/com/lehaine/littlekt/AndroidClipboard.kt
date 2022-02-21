package com.lehaine.littlekt

import android.content.ClipData
import android.content.ClipboardManager
import com.lehaine.littlekt.util.Clipboard

/**
 * @author Colton Daily
 * @date 2/21/2022
 */
class AndroidClipboard(val context: android.content.Context) : Clipboard {
    private val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager

    override val hasContents: Boolean
        get() = clipboard.hasPrimaryClip()
    override var contents: String?
        get() {
            val clip = clipboard.primaryClip ?: return null
            val text = clip.getItemAt(0).text ?: return null
            return text.toString()
        }
        set(value) {
            val data = ClipData.newPlainText(value, value)
            clipboard.setPrimaryClip(data)
        }
}