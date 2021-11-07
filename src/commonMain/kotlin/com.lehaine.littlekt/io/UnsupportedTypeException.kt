package com.lehaine.littlekt.io

import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class UnsupportedTypeException(val type: KClass<*>) : RuntimeException("Unsupported type '${type::class}'")