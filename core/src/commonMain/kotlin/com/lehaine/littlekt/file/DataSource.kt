package com.lehaine.littlekt.file

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
sealed class DataSource(val buffer: Buffer) {

    class FloatBufferDataSource(buffer: FloatBuffer) : DataSource(buffer)
    class ByteBufferDataSource(buffer: ByteBuffer) : DataSource(buffer)
    class ShortBufferDataSource(buffer: ShortBuffer) : DataSource(buffer)
    class IntBufferDataSource(buffer: IntBuffer) : DataSource(buffer)
}