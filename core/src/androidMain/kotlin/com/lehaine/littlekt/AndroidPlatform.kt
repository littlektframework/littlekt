package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Color

actual class LittleKtProps {
    var title: String = "LitteKt"
    var vSync: Boolean = true
    var backgroundColor = Color.CLEAR
}

actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    return LittleKtApp(
        AndroidContext(
            AndroidConfiguration(
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
    override val title: String,
    val vSync: Boolean,
    val backgroundColor: Color
) : ContextConfiguration()