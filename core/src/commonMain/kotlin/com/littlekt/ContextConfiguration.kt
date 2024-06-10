package com.littlekt

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
abstract class ContextConfiguration {
    abstract val title: String
    open val loadInternalResources: Boolean = true
}

/** When requesting an adapter, what is the preferred power usage. */
enum class PowerPreference {
    /** Lower power adapter. */
    LOW_POWER,

    /** High power adapter. */
    HIGH_POWER
}
