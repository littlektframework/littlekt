package com.littlekt.graphics.particle

import com.littlekt.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParticleEffectTests {

    @Test
    fun test_defaults_are_valid() {
        val effect = ParticleEffect()
        assertEquals(2048, effect.maxParticles)
        assertEquals(ParticleBlend.ALPHA, effect.blend)
    }

    @Test
    fun test_rejects_invalid_values() {
        assertFailsWith<IllegalArgumentException> { ParticleEffect(maxParticles = 0) }
        assertFailsWith<IllegalArgumentException> { ParticleEffect(rate = -1f) }
        assertFailsWith<IllegalArgumentException> { ParticleEffect(lifetime = 0f..1f) }
        assertFailsWith<IllegalArgumentException> { ParticleEffect(sizes = emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            ParticleEffect(colors = List(ParticleEffect.MAX_KEYS + 1) { Color.WHITE })
        }
    }

    @Test
    fun test_copy_changes_one_value() {
        val effect = ParticleEffect(rate = 10f)
        val faster = effect.copy(rate = 20f)
        assertEquals(20f, faster.rate)
        assertEquals(effect.lifetime, faster.lifetime)
    }

    @Test
    fun test_flipbook_defaults_to_every_frame() {
        val sheet = Flipbook(columns = 4, rows = 2)
        assertEquals(8, sheet.frames)
        assertEquals(0f, sheet.fps)
        assertEquals(true, sheet.blend)
    }

    @Test
    fun test_flipbook_rejects_invalid_values() {
        assertFailsWith<IllegalArgumentException> { Flipbook(columns = 0, rows = 2) }
        assertFailsWith<IllegalArgumentException> { Flipbook(columns = 2, rows = 2, frames = 5) }
        assertFailsWith<IllegalArgumentException> { Flipbook(columns = 2, rows = 2, frames = 0) }
        assertFailsWith<IllegalArgumentException> { Flipbook(columns = 2, rows = 2, fps = -1f) }
    }

    @Test
    fun test_flipbook_and_new_blends_fit_an_effect() {
        val flames = ParticleEffect(flipbook = Flipbook(4, 4, loop = true), blend = ParticleBlend.ADDITIVE)
        assertEquals(16, flames.flipbook?.frames)
        assertEquals(ParticleBlend.DISTORT, ParticleEffect(blend = ParticleBlend.DISTORT).blend)
        assertEquals(ParticleBlend.OPAQUE, ParticleEffect(blend = ParticleBlend.OPAQUE).blend)
    }
}
