package com.lehaine.littlekt.file

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
sealed class DataSource(val buffer: Buffer) {

    class Float32BufferDataSource(buffer: FloatBuffer) : DataSource(buffer)
    class Uint8BufferDataSource(buffer: ByteBuffer) : DataSource(buffer)
    class Uint16BufferDataSource(buffer: ShortBuffer) : DataSource(buffer)
    class Uint32BufferDataSource(buffer: IntBuffer) : DataSource(buffer)
}