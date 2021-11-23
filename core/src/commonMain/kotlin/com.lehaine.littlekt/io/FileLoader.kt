package com.lehaine.littlekt.io

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
interface FileLoader<T> {

    fun load(filename: String, handler: FileHandler): Content<T>
}