package com.lehaine.littlekt.io

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.graphics.TextureData
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
interface FileHandler {

    val application: Application

    fun <T> create(filename: String, value: T): Content<T>

    fun <T, R : Any> get(filename: String, rClazz: KClass<R>, map: (R) -> Content<T>): Content<T>

    fun <T : Any> get(filename: String, clazz: KClass<T>): Content<T>

    fun read(filename: String): Content<String>

    fun readData(filename: String): Content<ByteArray>

    fun readTextureData(filename: String): Content<TextureData>

    fun readSound(filename: String): Content<Sound>

    fun isFullyLoaded(): Boolean

    fun loadingProgress(): Float
}

inline fun <reified T : Any> FileHandler.get(filename: String): Content<T> = get(filename, T::class)