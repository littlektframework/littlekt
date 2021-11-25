package com.lehaine.littlekt.io

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
sealed class DataSource(val buffer: Buffer) {

    class Float32BufferDataSource(buffer: Float32Buffer) : DataSource(buffer)
    class Uint8BufferDataSource(buffer: Uint8Buffer) : DataSource(buffer)
    class Uint16BufferDataSource(buffer: Uint16Buffer) : DataSource(buffer)
    class Uint32BufferDataSource(buffer: Uint32Buffer) : DataSource(buffer)
}