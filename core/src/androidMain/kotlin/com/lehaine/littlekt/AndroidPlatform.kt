package com.lehaine.littlekt

import android.app.Activity
import com.lehaine.littlekt.graphics.Color

actual class LittleKtProps {
    var activity: Activity? = null
    var title: String = "LitteKt"
    var vSync: Boolean = true
    var backgroundColor = Color.CLEAR
}

actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    return LittleKtApp(
        AndroidContext(
            AndroidConfiguration(
                props.activity ?: error("Ensure to set 'activity' in 'LittleKtProps' when creating an application!"),
                props.title,
                props.vSync,
                props.backgroundColor
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
    val vSync: Boolean = true,
    val backgroundColor: Color = Color.CLEAR
) : ContextConfiguration()