package com.littlekt

import java.io.*
import java.util.zip.CRC32

/**
 * Extracts native libraries from the classpath and stores them temporarily.
 *
 * @author mzechner
 * @author Nathan Sweet
 * @author Noah Charlton
 * @author Colt Daily
 */
internal class SharedLibraryLoader {
    private object Platform {
        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val isLinux = System.getProperty("os.name").lowercase().contains("linux")
        val isAarch64 = System.getProperty("os.arch").lowercase().contains("aarch64")
    }

    private fun crc(input: InputStream?): String {
        check(input != null) { "Input cannot be null" }
        val crc = CRC32()
        val buffer = ByteArray(4096)
        runCatching {
            while (true) {
                val length = input.read(buffer)
                if (length == -1) break
                crc.update(buffer, 0, length)
            }
        }
        closeQuietly(input)
        return crc.value.toString(16)
    }

    private fun mapLibraryName(libraryName: String): String {
        if (Platform.isWindows) return "$libraryName.dll"
        if (Platform.isLinux) return "lib$libraryName.so"
        if (Platform.isMac) {
            return if (Platform.isAarch64) {
                "lib${libraryName}_aarch64.dylib"
            } else {
                "lib$libraryName.dylib"
            }
        }
        return libraryName
    }

    fun load(libraryName: String): File {
        val platformName = mapLibraryName(libraryName)
        try {
            return loadFile(platformName)
        } catch (ex: Throwable) {
            ex.printStackTrace()
            error(
                "Couldn't load shared library '$platformName' for target: ${System.getProperty("os.name")}"
            )
        }
    }

    private fun readFile(path: String): InputStream {
        return Thread.currentThread().contextClassLoader.getResourceAsStream(path)
            ?: error("Unable to read file for extraction: $path")
    }

    private fun extractFile(sourcePath: String, sourceCrc: String, extractedFile: File): File {
        var extractedCrc: String? = null
        if (extractedFile.exists()) {
            try {
                extractedCrc = crc(FileInputStream(extractedFile))
            } catch (ignored: FileNotFoundException) {}
        }

        // If file doesn't exist or the CRC doesn't match, extract it to the temp dir.
        if (extractedCrc == null || extractedCrc != sourceCrc) {
            var input: InputStream? = null
            var output: FileOutputStream? = null
            try {
                input = readFile(sourcePath)
                extractedFile.parentFile.mkdirs()
                output = FileOutputStream(extractedFile)
                val buffer = ByteArray(4096)
                while (true) {
                    val length = input.read(buffer)
                    if (length == -1) break
                    output.write(buffer, 0, length)
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
                error("Error extracting file: $sourcePath\nTo: ${extractedFile.absolutePath}")
            } finally {
                closeQuietly(input)
                closeQuietly(output)
            }
        }
        return extractedFile
    }

    private fun closeQuietly(closeable: Closeable?) {
        if (closeable == null) return
        try {
            closeable.close()
        } catch (e: IOException) {
            System.err.println("Failed to close dll: $e")
        }
    }

    @Suppress("UnsafeDynamicallyLoadedCode")
    private fun loadFile(sourcePath: String): File {
        val sourceCrc = crc(readFile(sourcePath))
        val fileName = File(sourcePath).name

        // Temp directory with username in path.
        var file =
            File(
                ("${System.getProperty("java.io.tmpdir")}/wgpu-natives/${System.getProperty("user.name")}/$sourceCrc"),
                fileName
            )
        val ex = loadFile(sourcePath, sourceCrc, file) ?: return file

        // System provided temp directory.
        try {
            file = File.createTempFile(sourceCrc, null)
            if (file.delete() && loadFile(sourcePath, sourceCrc, file) == null) return file
        } catch (ignored: Throwable) {}

        // User home.
        file = File("${System.getProperty("user.home")}/.wgpu-natives/$sourceCrc", fileName)
        if (loadFile(sourcePath, sourceCrc, file) == null) return file

        // Relative directory.
        file = File(".temp/$sourceCrc", fileName)
        if (loadFile(sourcePath, sourceCrc, file) == null) return file

        // Fallback to java.library.path location, eg for applets.
        file = File(System.getProperty("java.library.path"), sourcePath)
        if (file.exists()) {
            System.load(file.absolutePath)
            return file
        }
        throw ex
    }

    @Suppress("UnsafeDynamicallyLoadedCode")
    private fun loadFile(sourcePath: String, sourceCrc: String, extractedFile: File): Throwable? {
        return try {
            System.load(extractFile(sourcePath, sourceCrc, extractedFile).absolutePath)
            null
        } catch (ex: Throwable) {
            ex
        }
    }
}
