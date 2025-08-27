package com.littlekt.graphics.webgpu

import com.littlekt.util.jsObject

actual fun GPUColorDict(): GPUColorDict = jsObject().unsafeCast<GPUColorDict>()