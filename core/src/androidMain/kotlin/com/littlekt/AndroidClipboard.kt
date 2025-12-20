package com.littlekt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.littlekt.util.Clipboard

class AndroidClipboard(val context: Context) : Clipboard {

    private val clipboardManager: ClipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override val hasContents: Boolean
        get() = contents.isNullOrEmpty().not()

    override var contents: String?
        get() {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                return clip.getItemAt(0).text.toString()
            }
            return null
        }
        set(value) {
            if (value.isNullOrEmpty()) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
            } else {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("clipboard", value))
            }
        }
}