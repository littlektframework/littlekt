package com.littlekt.graphics

import com.littlekt.graphics.webgpu.Device
import com.littlekt.util.datastructure.internal.threadSafeMutableMapOf

private val mipMapGenerators = threadSafeMutableMapOf<Device, MipMapGenerator>()

/** Gets and existing or creates a new [MipMapGenerator] based off this [Device]. */
fun Device.getOrCreateMipMapGenerator() = mipMapGenerators.getOrPut(this) { MipMapGenerator(this) }

/** Releases the [MipMapGenerator] for this device, if it exists. */
fun Device.releaseMipMapGenerator() = mipMapGenerators[this]?.release()
