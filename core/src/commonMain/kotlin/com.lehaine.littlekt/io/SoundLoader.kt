package com.lehaine.littlekt.io

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class SoundLoader : FileLoader<Sound> {
    override fun load(filename: String, handler: FileHandler): Content<Sound> {
        return handler.readSound(filename)
    }
}