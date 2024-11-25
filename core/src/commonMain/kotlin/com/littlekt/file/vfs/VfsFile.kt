package com.littlekt.file.vfs

import com.littlekt.file.ByteBuffer
import com.littlekt.file.ByteSequenceStream
import com.littlekt.file.Vfs

/**
 * A virtual file or directory on a file system.
 *
 * @author Colton Daily
 * @date 12/20/2021
 */
data class VfsFile(val vfs: Vfs, val path: String) : VfsNamed(path.pathInfo) {
    /** The parent [VfsFile]. */
    val parent: VfsFile
        get() = VfsFile(vfs, folder)

    /** The root [VfsFile] of the [Vfs]. */
    val root: VfsFile
        get() = vfs.root

    /** The absolute path string of this file. */
    val absolutePath: String
        get() = vfs.getAbsolutePath(this.path)

    /**
     * An alias for `PathInfo(absolutePath)`.
     *
     * @see PathInfo
     */
    val absolutePathInfo: PathInfo
        get() = PathInfo(absolutePath)

    /** Reads the file into a [ByteBuffer]. */
    suspend fun read(): ByteBuffer = vfs.readBytes(path)

    /** Creates a [ByteSequenceStream] to read the file. */
    suspend fun readStream(): ByteSequenceStream = vfs.readStream(path)

    /** Reads the entire file into a [ByteArray]. */
    suspend fun readBytes(): ByteArray = read().toArray()

    /** Reads the entire file as a string. */
    suspend fun readString(): String = readBytes().decodeToString()

    /** Reads the entire file as a list of string for each new line. */
    suspend fun readLines(): List<String> = readBytes().decodeToString().split("\n")

    /** Reads file as a JSON string and decodes it to the specified type. */
    suspend inline fun <reified T> decodeFromString() = vfs.json.decodeFromString<T>(readString())

    /** @return the relative path to this file. */
    fun relativePathTo(relative: VfsFile): String? {
        if (relative.vfs != this.vfs) return null
        return this.pathInfo.relativePathTo(relative.pathInfo)
    }

    /** @return `true` if the path starts with `http://`, `https://` or `data:`. */
    fun isHttpUrl(): Boolean {
        // maybe use something less naive here?
        return path.startsWith("http://", true) ||
            path.startsWith("https://", true) ||
            path.startsWith("data:", true)
    }

    /** @return the [VfsFile] based off this files path. */
    operator fun get(path: String): VfsFile =
        VfsFile(vfs, this.path.pathInfo.combine(path.pathInfo).fullPath)
}
