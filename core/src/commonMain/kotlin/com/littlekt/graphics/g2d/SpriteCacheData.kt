package com.littlekt.graphics.g2d

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer

interface SpriteCacheData : Releasable{
    /**
     * Update this internal sprite static storage with the given data.
     *
     * @param data the sprite data to upload to the buffer
     * @return true if the storage buffer was recreated; false otherwise.
     */
    fun updateSpriteStaticStorage(data: FloatBuffer): Boolean

    /**
     * Update this internal sprite dynamic storage ith the given data.
     *
     * @param data the sprite data to upload to the buffer
     * @return true if the storage buffer was recreated; false otherwise.
     */
    fun updateSpriteDynamicStorage(data: FloatBuffer): Boolean
}