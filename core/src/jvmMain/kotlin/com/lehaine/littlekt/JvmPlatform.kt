package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var title: String = "LitteKt"
    var vSync: Boolean = true
}

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    return LittleKtApp(
        LwjglContext(
            JvmConfiguration(
                props.title,
                props.width,
                props.height,
                props.vSync
            )
        )
    )
}

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class JvmConfiguration(override val title: String, val width: Int, val height: Int, val vSync: Boolean) :
    ContextConfiguration()