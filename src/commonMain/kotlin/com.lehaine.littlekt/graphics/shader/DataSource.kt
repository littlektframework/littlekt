package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.io.FloatBuffer
import com.lehaine.littlekt.io.ShortBuffer

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
    class FloatBufferDataSource(val buffer: FloatBuffer) : DataSource()
    class ShortBufferDataSource(val buffer: ShortBuffer) : DataSource()
}