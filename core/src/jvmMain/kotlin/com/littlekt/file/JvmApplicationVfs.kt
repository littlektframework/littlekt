package com.littlekt.file

import com.littlekt.Context
import com.littlekt.log.Logger
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * @author Colton Daily
 * @date 8/23/2024
 */
class JvmApplicationVfs(context: Context, logger: Logger, baseDir: String) :
    JvmLocalVfs(context, logger, baseDir) {

    init {
        HttpCache.initCache(File(".httpCache"))
    }

    override fun openLocalStream(assetPath: String): InputStream {
        return FileInputStream(assetPath)
    }
}
