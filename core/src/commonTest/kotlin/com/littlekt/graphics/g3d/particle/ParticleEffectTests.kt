package com.littlekt.graphics.g3d.particle

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
}
