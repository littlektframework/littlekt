package com.littlekt.file

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import java.io.IOException

class AndroidKeyValueStorage(
    val context: Context
) : KeyValueStorage {
    private val sharedPreferences = context.getSharedPreferences("LittleKtPrefs", Context.MODE_PRIVATE)

    override fun store(key: String, data: ByteArray): Boolean {
        return try {
            val file = File(getKeyValueFilesDir(), key)
            file.writeBytes(data)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    override fun load(key: String): ByteBuffer? {
        return try {
            val file = File(getKeyValueFilesDir(), key)
            if (file.exists().not()) {
                return null
            }
            val bytes = file.readBytes()
            ByteBufferImpl(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("UseKtx")
    override fun store(key: String, data: String): Boolean {
        return sharedPreferences.edit().putString(key, data).commit()
    }

    override fun loadString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun getKeyValueFilesDir(): File {
        val filesDir = File(context.filesDir, "LittleKtData")
        if (filesDir.exists().not()) {
            filesDir.mkdirs()
        }
        return filesDir
    }
}