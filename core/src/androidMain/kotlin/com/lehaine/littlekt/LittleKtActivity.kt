package com.lehaine.littlekt

import android.app.Activity
import android.os.Bundle

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
abstract class LittleKtActivity : Activity() {

    abstract fun LittleKtProps.configureLittleKt()

    abstract fun createContextListener(context: Context): ContextListener

    override fun onCreate(savedInstanceState: Bundle?) {
        createLittleKtApp { configureLittleKt() }.start { createContextListener(it) }
        super.onCreate(savedInstanceState)
    }
}