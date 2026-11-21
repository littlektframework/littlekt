package com.littlekt.audio

/**
 * How a sound played with [AudioClip.playAt] gets quieter as it moves away from the
 * [AudioListener].
 *
 * @property refDistance the distance within which the sound plays at full volume
 * @property maxDistance the distance at which a [DistanceModel.LINEAR] sound is silent. The other
 *   models keep getting quieter past it.
 * @property rolloff how quickly the volume falls off past [refDistance]. `0` never falls off.
 * @property distanceModel the curve the volume falls off with
 * @author Colton Daily
 * @date 11/21/2026
 */
data class AudioAttenuation(
    val refDistance: Float = 1f,
    val maxDistance: Float = 100f,
    val rolloff: Float = 1f,
    val distanceModel: DistanceModel = DistanceModel.INVERSE,
) {
    init {
        require(refDistance > 0f) { "refDistance must be greater than 0!" }
        require(maxDistance > refDistance) { "maxDistance must be greater than refDistance!" }
        require(rolloff >= 0f) { "rolloff must not be negative!" }
    }

    companion object {
        /** Full volume within 1 unit, then falls off with [DistanceModel.INVERSE]. */
        val DEFAULT = AudioAttenuation()
    }
}

/** The curve a spatial sound's volume falls off with past [AudioAttenuation.refDistance]. */
enum class DistanceModel {
    /** `ref / (ref + rolloff * (distance - ref))`. Sounds natural, never reaches silence. */
    INVERSE,

    /** `1 - rolloff * (distance - ref) / (max - ref)`. Reaches silence at the max distance. */
    LINEAR,

    /** `(distance / ref) ^ -rolloff`. Falls off faster than [INVERSE] up close. */
    EXPONENTIAL,
}
