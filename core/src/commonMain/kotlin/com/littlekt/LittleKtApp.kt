package com.littlekt

/**
 * The base app from starting and stopping a [Context].
 *
 * @author Colton Daily
 * @date 11/17/2021
 */
class LittleKtApp(val context: Context) {

    /** Starts the [Context]. */
    fun start(gameBuilder: (app: Context) -> ContextListener): LittleKtApp {
        context.start(gameBuilder)
        return this
    }

    /** Closes the [Context] */
    fun close(): LittleKtApp {
        context.close()
        return this
    }
}

/** Properties related to creating a [LittleKtApp] */
expect class LittleKtProps

/**
 * Creates a new [LittleKtApp] containing [LittleKtProps] as the [ContextConfiguration] for building
 * a [Context].
 */
expect fun createLittleKtApp(action: LittleKtProps.() -> Unit = {}): LittleKtApp
