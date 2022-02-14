package com.lehaine.littlekt

import android.app.Activity
import com.lehaine.littlekt.graphics.Color

actual class LittleKtProps {
    var activity: Activity? = null
    var title: String = "LitteKt"
    var backgroundColor = Color.CLEAR
    var showStatusBar = true
    var useImmersiveMode = true
    var useWakeLock = true
}

actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    return LittleKtApp(
        AndroidContext(
            AndroidConfiguration(
                props.activity ?: error("Ensure to set 'activity' in 'LittleKtProps' when creating an application!"),
                props.title,
                props.backgroundColor,
                props.showStatusBar,
                props.useImmersiveMode,
                props.useWakeLock
            )
        )
    )
}

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
class AndroidConfiguration(
    val activity: Activity,
    override val title: String,
    val backgroundColor: Color = Color.CLEAR,
    val showStatusBar: Boolean = true,
    val useImmersiveMode: Boolean = true,
    val useWakeLock: Boolean = true
) : ContextConfiguration()