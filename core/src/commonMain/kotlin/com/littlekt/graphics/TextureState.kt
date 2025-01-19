package com.littlekt.graphics

/**
 * @author Colton Daily
 * @date 1/19/2025
 */
enum class TextureState {
    /** The texture is unloaded and has not been written to the GPU. */
    UNLOADED,

    /** The texture is currently loading the underlying data. */
    LOADING,

    /** The texture has been written to the GPU. */
    LOADED,
}
