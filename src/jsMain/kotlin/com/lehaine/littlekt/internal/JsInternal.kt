package com.lehaine.littlekt.internal

import kotlin.js.Date

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
internal actual fun epochMillis(): Long = Date.now().toLong()