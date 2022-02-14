package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.LittleKtActivity
import com.lehaine.littlekt.LittleKtProps
import com.lehaine.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 2/11/2022
 */
class AppActivity : LittleKtActivity() {

    override fun LittleKtProps.configureLittleKt() {
        activity = this@AppActivity
        backgroundColor = Color.DARK_GRAY
    }

    override fun createContextListener(context: Context): ContextListener {
        return DisplayTest(context)
    }
}