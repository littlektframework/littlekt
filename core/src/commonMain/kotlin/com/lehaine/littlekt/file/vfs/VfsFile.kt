package com.lehaine.littlekt.file.vfs

import com.lehaine.littlekt.file.ByteBuffer
import com.lehaine.littlekt.file.ByteSequenceStream
import com.lehaine.littlekt.file.Vfs
import kotlinx.serialization.decodeFromString

/**
 * A virtual file or directory on a file system.
 * @author Colton Daily
 * @date 12/20/2021
 */
data class VfsFile(val vfs: Vfs, val path: String) : VfsNamed(path.pathInfo) {
    val parent: VfsFile get() = VfsFile(vfs, folder)
    val root: VfsFile get() = vfs.root
    val absolutePath: String get() = vfs.getAbsolutePath(this.path)
    val absolutePathInfo: PathInfo get() = PathInfo(absolutePath)

    suspend fun read(): ByteBuffer = vfs.readBytes(path)
    suspend fun readStream(): ByteSequenceStream = vfs.readStream(path)
    suspend fun readBytes(): ByteArray = read().toArray()
    suspend fun readString(): String = readBytes().decodeToString()
    suspend fun readLines(): List<String> = readBytes().decodeToString().split("\n")
    suspend inline fun <reified T> decodeFromString() = vfs.json.decodeFromString<T>(readString())

    fun writeKeystore(data: ByteArray) = vfs.store(pathInfo.baseName, data)
    fun writeKeystore(data: String) = vfs.store(pathInfo.baseName, data)
    fun readKeystore() = vfs.loadString(pathInfo.baseName)
    fun readKeystoreBytes() = vfs.load(pathInfo.baseName)

    fun relativePathTo(relative: VfsFile): String? {
        if (relative.vfs != this.vfs) return null
        return this.pathInfo.relativePathTo(relative.pathInfo)
    }

    fun isHttpUrl(): Boolean {
        // maybe use something less naive here?
        return path.startsWith("http://", true) ||
                path.startsWith("https://", true) ||
                path.startsWith("data:", true)
    }

    operator fun get(path: String): VfsFile =
        VfsFile(vfs, this.path.pathInfo.combine(path.pathInfo).fullPath)
}