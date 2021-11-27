package com.lehaine.littlekt.util

import com.lehaine.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 11/27/2021
 */
class LazyMat4(val update: (Mat4) -> Unit) {
    private val mat = Mat4()

    var isDirty = true

    fun get(): Mat4 {
        if (isDirty) {
            update(mat)
            isDirty = false
        }
        return mat
    }
}