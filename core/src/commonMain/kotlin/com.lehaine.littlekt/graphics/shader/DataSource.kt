package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.io.Float32Buffer
import com.lehaine.littlekt.io.Uint16Buffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
sealed class DataSource {

    class FloatDataSource(val floats: FloatArray) : DataSource()
    class IntDataSource(val ints: IntArray) : DataSource()
    class ShortDataSource(val shorts: ShortArray) : DataSource()
    class UIntDataSource(val ints: IntArray) : DataSource()
    class DoubleDataSource(val double: DoubleArray) : DataSource()
    class Float32BufferDataSource(val buffer: Float32Buffer) : DataSource()
    class Uint16BufferDataSource(val buffer: Uint16Buffer) : DataSource()
}