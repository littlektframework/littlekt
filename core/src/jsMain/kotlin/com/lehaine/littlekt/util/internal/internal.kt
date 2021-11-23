package com.lehaine.littlekt.util.internal

import kotlin.js.Date

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
internal actual fun epochMillis(): Long = Date.now().toLong()