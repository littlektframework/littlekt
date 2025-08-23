package com.littlekt.graphics.webgpu

import com.littlekt.util.jsObject

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
actual fun rgbaToColorDict(values: FloatArray): GPUColorDict {
    return GPUColorDict().apply {
        r = values.getOrNull(0)?.toDouble() ?: 0.0
        g = values.getOrNull(1)?.toDouble() ?: 0.0
        b = values.getOrNull(2)?.toDouble() ?: 0.0
        a = values.getOrNull(3)?.toDouble() ?: 1.0
    }
}

actual fun GPUColorDict(): GPUColorDict = jsObject().unsafeCast<GPUColorDict>()