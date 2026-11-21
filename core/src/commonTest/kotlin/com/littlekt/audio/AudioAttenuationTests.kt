package com.littlekt.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AudioAttenuationTests {

    @Test
    fun test_defaults() {
        val attenuation = AudioAttenuation.DEFAULT
        assertEquals(1f, attenuation.refDistance)
        assertEquals(DistanceModel.INVERSE, attenuation.distanceModel)
    }

    @Test
    fun test_rejects_invalid_distances() {
        assertFailsWith<IllegalArgumentException> { AudioAttenuation(refDistance = 0f) }
        assertFailsWith<IllegalArgumentException> { AudioAttenuation(refDistance = 5f, maxDistance = 5f) }
        assertFailsWith<IllegalArgumentException> { AudioAttenuation(rolloff = -1f) }
    }
}
