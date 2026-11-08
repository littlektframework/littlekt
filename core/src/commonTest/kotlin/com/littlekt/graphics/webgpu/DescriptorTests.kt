package com.littlekt.graphics.webgpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DescriptorTests {

    @Test
    fun test_texture_descriptor_defaults() {
        val descriptor =
            TextureDescriptor(
                Extent3D(4, 4, 1),
                format = TextureFormat.RGBA8_UNORM,
                usage = TextureUsage.RENDER_ATTACHMENT,
            )
        assertEquals(1, descriptor.mipLevelCount)
        assertEquals(1, descriptor.sampleCount)
        assertEquals(TextureDimension.D2, descriptor.dimension)
    }

    @Test
    fun test_color_target_state_defaults() {
        val target = ColorTargetState(TextureFormat.RGBA8_UNORM)
        assertNull(target.blendState)
        assertEquals(ColorWriteMask.ALL, target.writeMask)
    }

    @Test
    fun test_buffer_descriptor_defaults() {
        assertFalse(BufferDescriptor("buffer", 16, BufferUsage.UNIFORM).mappedAtCreation)
    }
}
