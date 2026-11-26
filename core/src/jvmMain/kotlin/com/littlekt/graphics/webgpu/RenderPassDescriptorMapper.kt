package com.littlekt.graphics.webgpu

import com.littlekt.graphics.Color
import ffi.MemoryAllocator
import ffi.NativeAddress
import io.ygdrasil.wgpu.WGPUColor
import io.ygdrasil.wgpu.WGPURenderPassColorAttachment
import io.ygdrasil.wgpu.WGPURenderPassDepthStencilAttachment
import io.ygdrasil.wgpu.WGPURenderPassDescriptor
import io.ygdrasil.wgpu.WGPUStringView
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_DOUBLE
import java.lang.foreign.ValueLayout.JAVA_FLOAT
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG

/**
 * A native [WGPURenderPassDescriptor] that is written again for each render pass instead of
 * allocating a new one, since wgpu copies the descriptor when a render pass begins. Each thread has
 * its own, from [get].
 *
 * @author Colton Daily
 * @date 11/26/2026
 */
internal class ReusableRenderPassDescriptor private constructor() {
    private val memory = MemoryAllocator()
    private val descriptor = memory.allocate(RENDER_PASS_DESCRIPTOR_SIZE).handler
    private val colors = memory.allocate(COLOR_ATTACHMENT_SIZE * MAX_COLOR_ATTACHMENTS).handler
    private val depthStencil = memory.allocate(DEPTH_STENCIL_ATTACHMENT_SIZE).handler
    private val label = memory.allocate(MAX_LABEL_BYTES.toLong()).handler
    private val native: WGPURenderPassDescriptor = WGPURenderPassDescriptor(NativeAddress(descriptor))

    /** @return the native descriptor of [input], valid until the next call on this thread. */
    fun write(input: RenderPassDescriptor): WGPURenderPassDescriptor {
        val count = input.colorAttachments.size
        require(count <= MAX_COLOR_ATTACHMENTS) {
            "A render pass can have at most $MAX_COLOR_ATTACHMENTS color attachments!"
        }
        val d = descriptor
        d.set(ADDRESS, WGPURenderPassDescriptor.nextInChainOffset, MemorySegment.NULL)
        writeLabel(input.label)
        d.set(JAVA_LONG, WGPURenderPassDescriptor.colorAttachmentCountOffset, count.toLong())
        d.set(
            ADDRESS,
            WGPURenderPassDescriptor.colorAttachmentsOffset,
            if (count > 0) colors else MemorySegment.NULL,
        )
        for (i in 0 until count) {
            writeColor(input.colorAttachments[i], i * COLOR_ATTACHMENT_SIZE)
        }
        val depth = input.depthStencilAttachment
        if (depth != null) writeDepthStencil(depth)
        d.set(
            ADDRESS,
            WGPURenderPassDescriptor.depthStencilAttachmentOffset,
            if (depth != null) depthStencil else MemorySegment.NULL,
        )
        d.set(ADDRESS, WGPURenderPassDescriptor.occlusionQuerySetOffset, MemorySegment.NULL)
        d.set(ADDRESS, WGPURenderPassDescriptor.timestampWritesOffset, MemorySegment.NULL)
        return native
    }

    private fun writeLabel(text: String?) {
        val data = WGPURenderPassDescriptor.labelOffset + WGPUStringView.dataOffset
        val length = WGPURenderPassDescriptor.labelOffset + WGPUStringView.lengthOffset
        if (text == null) {
            descriptor.set(ADDRESS, data, MemorySegment.NULL)
            descriptor.set(JAVA_LONG, length, 0L)
            return
        }
        val size = minOf(text.length, MAX_LABEL_BYTES)
        for (i in 0 until size) {
            val c = text[i].code
            label.set(JAVA_BYTE, i.toLong(), if (c < 128) c.toByte() else QUESTION_MARK)
        }
        descriptor.set(ADDRESS, data, label)
        descriptor.set(JAVA_LONG, length, size.toLong())
    }

    private fun writeColor(input: RenderPassColorAttachmentDescriptor, base: Long) {
        val c = colors
        val resolveTarget = input.resolveTarget
        val depthSlice = input.depthSlice
        val color = input.clearColor
        c.set(ADDRESS, base + WGPURenderPassColorAttachment.nextInChainOffset, MemorySegment.NULL)
        c.set(ADDRESS, base + WGPURenderPassColorAttachment.viewOffset, input.view.segment.handler.handler)
        c.set(JAVA_INT, base + WGPURenderPassColorAttachment.depthSliceOffset, depthSlice ?: -1)
        c.set(
            ADDRESS,
            base + WGPURenderPassColorAttachment.resolveTargetOffset,
            if (resolveTarget != null) resolveTarget.segment.handler.handler else MemorySegment.NULL,
        )
        c.set(JAVA_INT, base + WGPURenderPassColorAttachment.loadOpOffset, input.loadOp.nativeVal.toInt())
        c.set(JAVA_INT, base + WGPURenderPassColorAttachment.storeOpOffset, input.storeOp.nativeVal.toInt())
        val clear = base + WGPURenderPassColorAttachment.clearValueOffset
        if (color != null) {
            c.set(JAVA_DOUBLE, clear + WGPUColor.rOffset, color.r.toDouble())
            c.set(JAVA_DOUBLE, clear + WGPUColor.gOffset, color.g.toDouble())
            c.set(JAVA_DOUBLE, clear + WGPUColor.bOffset, color.b.toDouble())
            c.set(JAVA_DOUBLE, clear + WGPUColor.aOffset, color.a.toDouble())
        } else {
            c.set(JAVA_DOUBLE, clear + WGPUColor.rOffset, 0.0)
            c.set(JAVA_DOUBLE, clear + WGPUColor.gOffset, 0.0)
            c.set(JAVA_DOUBLE, clear + WGPUColor.bOffset, 0.0)
            c.set(JAVA_DOUBLE, clear + WGPUColor.aOffset, 0.0)
        }
    }

    private fun writeDepthStencil(input: RenderPassDepthStencilAttachmentDescriptor) {
        val d = depthStencil
        d.set(ADDRESS, WGPURenderPassDepthStencilAttachment.viewOffset, input.view.segment.handler.handler)
        d.set(JAVA_INT, WGPURenderPassDepthStencilAttachment.depthLoadOpOffset, input.depthLoadOp.nativeOrZero())
        d.set(JAVA_INT, WGPURenderPassDepthStencilAttachment.depthStoreOpOffset, input.depthStoreOp.nativeOrZero())
        d.set(JAVA_FLOAT, WGPURenderPassDepthStencilAttachment.depthClearValueOffset, input.depthClearValue)
        d.set(JAVA_INT, WGPURenderPassDepthStencilAttachment.depthReadOnlyOffset, if (input.depthReadOnly) 1 else 0)
        d.set(JAVA_INT, WGPURenderPassDepthStencilAttachment.stencilLoadOpOffset, input.stencilLoadOp.nativeOrZero())
        d.set(JAVA_INT, WGPURenderPassDepthStencilAttachment.stencilStoreOpOffset, input.stencilStoreOp.nativeOrZero())
        d.set(JAVA_INT, WGPURenderPassDepthStencilAttachment.stencilClearValueOffset, input.stencilClearValue)
        d.set(JAVA_INT, WGPURenderPassDepthStencilAttachment.stencilReadOnlyOffset, if (input.stencilReadOnly) 1 else 0)
    }

    private fun LoadOp?.nativeOrZero(): Int = if (this == null) 0 else nativeVal.toInt()

    private fun StoreOp?.nativeOrZero(): Int = if (this == null) 0 else nativeVal.toInt()

    companion object {
        private const val RENDER_PASS_DESCRIPTOR_SIZE = 64L
        private const val COLOR_ATTACHMENT_SIZE = 72L
        private const val DEPTH_STENCIL_ATTACHMENT_SIZE = 40L
        private const val MAX_COLOR_ATTACHMENTS = 8
        private const val MAX_LABEL_BYTES = 256
        private const val QUESTION_MARK = '?'.code.toByte()

        private val descriptors: ThreadLocal<ReusableRenderPassDescriptor> =
            ThreadLocal.withInitial { ReusableRenderPassDescriptor() }

        /** @return this thread's [ReusableRenderPassDescriptor]. */
        fun get(): ReusableRenderPassDescriptor = descriptors.get()
    }
}

internal fun MemoryAllocator.map(input: RenderPassDescriptor): WGPURenderPassDescriptor =
    WGPURenderPassDescriptor.allocate(this).also { output ->
        if (input.label != null) map(input.label, output.label)

        if (input.colorAttachments.isNotEmpty()) {
            output.colorAttachmentCount = input.colorAttachments.size.toULong()
            output.colorAttachments = WGPURenderPassColorAttachment.allocateArray(
                this,
                output.colorAttachmentCount.toUInt()
            ) { index, value ->
                map(input.colorAttachments[index.toInt()], value)
            }
        }

        if (input.depthStencilAttachment != null) output.depthStencilAttachment = map(input.depthStencilAttachment)
        //TODO map this var occlusionQuerySet: GPUQuerySet?
        //TODO map this var timestampWrites: GPURenderPassTimestampWrites?
        //TODO map this var maxDrawCount: GPUSize64
        // check WGPURenderPassDescriptorMaxDrawCount
    }

internal fun map(input: RenderPassColorAttachmentDescriptor, output: WGPURenderPassColorAttachment) {
    output.view = input.view.segment
    output.loadOp = input.loadOp.nativeVal
    output.storeOp = input.storeOp.nativeVal
    output.depthSlice = input.depthSlice?.toUInt() ?: UInt.MAX_VALUE
    val resolveTarget = input.resolveTarget
    if (resolveTarget != null) output.resolveTarget = resolveTarget.segment
    if (input.clearColor != null) map(input.clearColor, output.clearValue)
}

internal fun MemoryAllocator.map(input: RenderPassDepthStencilAttachmentDescriptor): WGPURenderPassDepthStencilAttachment =
    WGPURenderPassDepthStencilAttachment.allocate(this).also { output ->
        output.view = input.view.segment
        output.depthClearValue = input.depthClearValue

        if (input.depthLoadOp != null) output.depthLoadOp = input.depthLoadOp.nativeVal
        if (input.depthStoreOp != null) output.depthStoreOp = input.depthStoreOp.nativeVal
        output.depthReadOnly = input.depthReadOnly
        output.stencilClearValue = input.stencilClearValue.toUInt()
        if (input.stencilLoadOp != null) output.stencilLoadOp = input.stencilLoadOp.nativeVal
        if (input.stencilStoreOp != null) output.stencilStoreOp = input.stencilStoreOp.nativeVal
        output.stencilReadOnly = input.stencilReadOnly
    }

internal fun map(input: Color, output: WGPUColor) {
    output.r = input.r.toDouble()
    output.g = input.g.toDouble()
    output.b = input.b.toDouble()
    output.a = input.a.toDouble()
}