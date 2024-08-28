package com.littlekt.file

import com.littlekt.Context
import com.littlekt.file.vfs.VfsFile
import com.littlekt.file.vfs.lightCombine
import com.littlekt.file.vfs.pathInfo
import com.littlekt.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * A virtual file system that handles loading and streaming data and files.
 *
 * @author Colton Daily
 * @date 11/6/2021
 */
abstract class Vfs(val context: Context, val logger: Logger, val baseDir: String = "") :
    CoroutineScope {
    /** The root [VfsFile] that this [Vfs] starts from. */
    val root: VfsFile
        get() = VfsFile(this, baseDir)

    abstract val json: Json

    protected open val absolutePath: String
        get() = ""

    /** Cancels this vfs job. */
    open fun close() = Unit

    /**
     * Get the absolute path of the queried [path] based off the Vfs path.
     *
     * @param path the path to retrieve the absolute path for
     */
    open fun getAbsolutePath(path: String) =
        absolutePath.pathInfo.lightCombine(path.pathInfo).fullPath

    protected open fun isHttpAsset(assetPath: String): Boolean =
        // maybe use something less naive here?
        assetPath.startsWith("http://", true) ||
            assetPath.startsWith("https://", true) ||
            assetPath.startsWith("data:", true)

    /**
     * Launches a new coroutine using this vfs coroutine context. Use this to load assets
     * asynchronously.
     */
    fun launch(block: suspend Vfs.() -> Unit) {
        (this as CoroutineScope).launch { block.invoke(this@Vfs) }
    }

    abstract suspend fun readBytes(assetPath: String): ByteBuffer

    /**
     * Opens a stream to a file into a [ByteSequenceStream].
     *
     * @param assetPath the path to file
     * @return the byte input stream
     */
    abstract suspend fun readStream(assetPath: String): ByteSequenceStream

    operator fun get(path: String) = root[path]
}
