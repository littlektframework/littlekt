package com.littlekt.graphics.particle

/**
 * Spawns particles from another [emitter] when particles of the emitter it is set on die or are
 * born, such as a burst of sparks where each firework fades out. Spawning happens on the GPU, so
 * any number of particles can trigger it.
 *
 * Particles of [emitter] spawn in world space at each event's position, using its
 * [ParticleEffect], and keep [ParticleEffect.inheritVelocity] of the triggering particle's
 * velocity. Simulate & draw [emitter] like any other emitter.
 *
 * @property emitter the emitter whose particles are spawned. Must use the same [ParticleRenderer].
 * @property trigger when particles of the parent spawn [count] particles of [emitter]
 * @property count the particles of [emitter] spawned for each event
 * @property maxEvents the most events handled in a single simulation. Extra events are dropped.
 * @author Colton Daily
 * @date 11/15/2026
 */
data class SubEmitter(
    val emitter: ParticleEmitter,
    val trigger: SubEmitterTrigger = SubEmitterTrigger.DEATH,
    val count: Int = 8,
    val maxEvents: Int = 256,
) {
    init {
        require(count > 0) { "count must be greater than 0!" }
        require(maxEvents > 0) { "maxEvents must be greater than 0!" }
    }
}

/**
 * When a [SubEmitter] spawns.
 *
 * @author Colton Daily
 * @date 11/15/2026
 */
enum class SubEmitterTrigger {
    /** When a particle reaches the end of its lifetime. */
    DEATH,

    /** When a particle spawns. */
    BIRTH,
}
