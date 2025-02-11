package com.littlekt.graphics.g3d

import com.littlekt.graphics.g3d.util.CameraComplexBuffers
import com.littlekt.graphics.g3d.util.CameraLightBuffers
import com.littlekt.graphics.g3d.util.LightBuffer
import com.littlekt.graphics.webgpu.Device

/** @return a new [PBREnvironment] that uses an underlying [CameraLightBuffers]. */
fun PBREnvironment(device: Device, maxLightCount: Int = 1024): PBREnvironment =
    PBREnvironment(CameraLightBuffers(device, LightBuffer(device, maxLightCount)))

/** @return a new [Environment] that uses an underlying [CameraComplexBuffers]. */
fun UnlitEnvironment(device: Device): Environment = Environment(CameraComplexBuffers(device))
