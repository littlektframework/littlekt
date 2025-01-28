package com.littlekt.graphics.g2d

import com.littlekt.math.Mat4
import io.ygdrasil.webgpu.RenderPassEncoder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Invokes [Batch.begin], [action] and then immediately flushes with [Batch.flush] and then
 * [Batch.end].
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Batch> T.use(
    renderPassEncoder: RenderPassEncoder,
    projectionMatrix: Mat4? = null,
    action: (T) -> Unit
) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    begin(projectionMatrix)
    action(this)
    flush(renderPassEncoder)
    end()
}

/**
 * Invokes [Batch.begin], [action] and then [Batch.end]. You are responsible for invoking
 * [Batch.flush]. This only begins and ends this batch.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Batch> T.use(projectionMatrix: Mat4? = null, action: (T) -> Unit) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    begin(projectionMatrix)
    action(this)
    end()
}
