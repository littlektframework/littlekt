package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readLDtkMap

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapTest(context: Context) : ContextListener(context) {

    init {
        vfs.launch {
            val map = resourcesVfs["ldtk/sample.ldtk"].readLDtkMap(true)
        }
    }
}