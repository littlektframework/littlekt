package com.littlekt

/** Properties related to creating a [LittleKtApp] */
actual class LittleKtProps

/**
 * Creates a new [LittleKtApp] containing [LittleKtProps] as the [ContextConfiguration] for building
 * a [Context].
 */
actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    TODO("Not yet implemented")
}
