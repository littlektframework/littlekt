package com.lehaine.littlekt

import android.app.Activity
import android.os.Bundle

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
abstract class LittleKtActivity : Activity() {
    private var context: AndroidContext? = null

    abstract fun LittleKtProps.configureLittleKt()

    abstract fun createContextListener(context: Context): ContextListener

    override fun onCreate(savedInstanceState: Bundle?) {
        createLittleKtApp { configureLittleKt() }.start {
            context = it as AndroidContext
            createContextListener(it)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        context?.resume()
        super.onResume()
    }

    override fun onPause() {
        context?.pause()
        super.onPause()
    }
}