package com.littlekt.graphics.g3d

import com.littlekt.graphics.g3d.util.CameraLightBuffers
import com.littlekt.graphics.g3d.util.CameraSimpleBuffers
import com.littlekt.graphics.g3d.util.LightBuffer
import com.littlekt.graphics.webgpu.Device

/** @return a new [PBREnvironment] that uses an underlying [CameraLightBuffers]. */
fun PBREnvironment(device: Device): Environment =
    PBREnvironment(CameraLightBuffers(device, LightBuffer(device, 10)))

/** @return a new [Environment] that uses an underlying [CameraSimpleBuffers]. */
fun UnlitEnvironment(device: Device): Environment = Environment(CameraSimpleBuffers(device))
