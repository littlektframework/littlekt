package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.*
import com.littlekt.log.Logger
import com.littlekt.resources.BufferResourceInfo
import com.littlekt.resources.TextureResourceInfo
import com.littlekt.wgpu.*
import com.littlekt.wgpu.WGPU.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

actual class Device(val segment: MemorySegment) : Releasable {

    actual val queue: Queue by lazy { Queue(wgpuDeviceGetQueue(segment)) }

    actual val features: List<Feature> by lazy {
        val list = mutableListOf<Feature>()
        Feature.entries.forEach {
            val result = wgpuDeviceHasFeature(segment, it.nativeVal)
            if (result == 1) {
                list += it
            }
        }
        list.toList()
    }

    actual val limits: Limits by lazy {
        Arena.ofConfined().use { scope ->
            val supported = WGPUSupportedLimits.allocate(scope)
            wgpuDeviceGetLimits(segment, supported)
            val desc = WGPUSupportedLimits.limits(supported)
            Limits(
                maxTextureDimension1D = WGPULimits.maxTextureDimension1D(desc),
                maxTextureDimension2D = WGPULimits.maxTextureDimension2D(desc),
                maxTextureDimension3D = WGPULimits.maxTextureDimension3D(desc),
                maxTextureArrayLayers = WGPULimits.maxTextureArrayLayers(desc),
                maxBindGroups = WGPULimits.maxBindGroups(desc),
                maxBindGroupsPlusVertexBuffers = WGPULimits.maxBindGroupsPlusVertexBuffers(desc),
                maxBindingsPerBindGroup = WGPULimits.maxBindingsPerBindGroup(desc),
                maxDynamicUniformBuffersPerPipelineLayout =
                    WGPULimits.maxDynamicUniformBuffersPerPipelineLayout(desc),
                maxDynamicStorageBuffersPerPipelineLayout =
                    WGPULimits.maxDynamicStorageBuffersPerPipelineLayout(desc),
                maxSampledTexturesPerShaderStage =
                    WGPULimits.maxSampledTexturesPerShaderStage(desc),
                maxSamplersPerShaderStage = WGPULimits.maxSamplersPerShaderStage(desc),
                maxStorageBuffersPerShaderStage = WGPULimits.maxStorageBuffersPerShaderStage(desc),
                maxStorageTexturesPerShaderStage =
                    WGPULimits.maxStorageTexturesPerShaderStage(desc),
                maxUniformBuffersPerShaderStage = WGPULimits.maxUniformBuffersPerShaderStage(desc),
                maxUniformBufferBindingSize = WGPULimits.maxUniformBufferBindingSize(desc),
                maxStorageBufferBindingSize = WGPULimits.maxStorageBufferBindingSize(desc),
                minUniformBufferOffsetAlignment = WGPULimits.minUniformBufferOffsetAlignment(desc),
                minStorageBufferOffsetAlignment = WGPULimits.minStorageBufferOffsetAlignment(desc),
                maxVertexBuffers = WGPULimits.maxVertexBuffers(desc),
                maxBufferSize = WGPULimits.maxBufferSize(desc),
                maxVertexAttributes = WGPULimits.maxVertexAttributes(desc),
                maxVertexBufferArrayStride = WGPULimits.maxVertexBufferArrayStride(desc),
                maxInterStageShaderComponents = WGPULimits.maxInterStageShaderComponents(desc),
                maxInterStageShaderVariables = WGPULimits.maxInterStageShaderVariables(desc),
                maxColorAttachments = WGPULimits.maxColorAttachments(desc),
                maxColorAttachmentBytesPerSample =
                    WGPULimits.maxColorAttachmentBytesPerSample(desc),
                maxComputeWorkgroupStorageSize = WGPULimits.maxComputeWorkgroupStorageSize(desc),
                maxComputeInvocationsPerWorkgroup =
                    WGPULimits.maxComputeInvocationsPerWorkgroup(desc),
                maxComputeWorkgroupSizeX = WGPULimits.maxComputeWorkgroupSizeX(desc),
                maxComputeWorkgroupSizeY = WGPULimits.maxComputeWorkgroupSizeY(desc),
                maxComputeWorkgroupSizeZ = WGPULimits.maxComputeWorkgroupSizeZ(desc),
                maxComputeWorkgroupsPerDimension = WGPULimits.maxComputeWorkgroupsPerDimension(desc),
            )
        }
    }

    actual fun createShaderModule(src: String): ShaderModule {
        return Arena.ofConfined().use { scope ->
            val desc = WGPUShaderModuleDescriptor.allocate(scope)
            val wgsl = WGPUShaderModuleWGSLDescriptor.allocate(scope)
            val wgslChain = WGPUShaderModuleWGSLDescriptor.chain(wgsl)

            WGPUChainedStruct.next(wgslChain, WGPU_NULL)
            WGPUChainedStruct.sType(wgslChain, WGPUSType_ShaderModuleWGSLDescriptor())
            WGPUShaderModuleWGSLDescriptor.code(wgsl, scope.allocateFrom(src))
            WGPUShaderModuleDescriptor.nextInChain(desc, wgslChain)
            ShaderModule(wgpuDeviceCreateShaderModule(segment, desc))
        }
    }

    actual fun createRenderPipeline(desc: RenderPipelineDescriptor): RenderPipeline {
        return Arena.ofConfined().use { scope ->
            val fragDesc =
                if (desc.fragment != null) {
                    val fragDesc = WGPUFragmentState.allocate(scope)
                    WGPUFragmentState.module(fragDesc, desc.fragment.module.segment)
                    WGPUFragmentState.entryPoint(
                        fragDesc,
                        desc.fragment.entryPoint.toNativeString(scope),
                    )

                    val targets =
                        WGPUColorTargetState.allocateArray(
                            desc.fragment.targets.size.toLong(),
                            scope,
                        )
                    desc.fragment.targets.forEach { colorTargetState ->
                        if (colorTargetState.blendState == null) {
                            TODO("Null blend states aren't currently supported.")
                        }
                        val blendState = WGPUBlendState.allocate(scope)
                        val colorBlend = WGPUBlendState.color(blendState)
                        val alphaBlend = WGPUBlendState.alpha(blendState)
                        WGPUBlendComponent.srcFactor(
                            colorBlend,
                            colorTargetState.blendState.color.srcFactor.nativeVal,
                        )
                        WGPUBlendComponent.dstFactor(
                            colorBlend,
                            colorTargetState.blendState.color.dstFactor.nativeVal,
                        )
                        WGPUBlendComponent.operation(
                            colorBlend,
                            colorTargetState.blendState.color.operation.nativeVal,
                        )
                        WGPUBlendComponent.srcFactor(
                            alphaBlend,
                            colorTargetState.blendState.alpha.srcFactor.nativeVal,
                        )
                        WGPUBlendComponent.dstFactor(
                            alphaBlend,
                            colorTargetState.blendState.alpha.dstFactor.nativeVal,
                        )
                        WGPUBlendComponent.operation(
                            alphaBlend,
                            colorTargetState.blendState.alpha.operation.nativeVal,
                        )

                        WGPUColorTargetState.format(targets, colorTargetState.format.nativeVal)
                        WGPUColorTargetState.writeMask(
                            targets,
                            colorTargetState.writeMask.usageFlag,
                        )
                        WGPUColorTargetState.blend(targets, blendState)
                    }
                    WGPUFragmentState.targets(fragDesc, targets)
                    WGPUFragmentState.targetCount(fragDesc, desc.fragment.targets.size.toLong())
                    fragDesc
                } else {
                    WGPU_NULL
                }

            val buffers =
                desc.vertex.buffers.mapToNativeEntries(
                    scope,
                    WGPUVertexBufferLayout.sizeof(),
                    WGPUVertexBufferLayout::allocateArray,
                ) { bufferLayout, nativeBufferLayout ->
                    val attributes =
                        bufferLayout.attributes.mapToNativeEntries(
                            scope,
                            WGPUVertexAttribute.sizeof(),
                            WGPUVertexAttribute::allocateArray,
                        ) { attribute, nativeAttribute ->
                            WGPUVertexAttribute.shaderLocation(
                                nativeAttribute,
                                attribute.shaderLocation,
                            )
                            WGPUVertexAttribute.format(nativeAttribute, attribute.format.nativeVal)
                            WGPUVertexAttribute.offset(nativeAttribute, attribute.offset)
                        }

                    WGPUVertexBufferLayout.arrayStride(nativeBufferLayout, bufferLayout.arrayStride)
                    WGPUVertexBufferLayout.stepMode(
                        nativeBufferLayout,
                        bufferLayout.stepMode.nativeVal,
                    )
                    WGPUVertexBufferLayout.attributeCount(
                        nativeBufferLayout,
                        bufferLayout.attributes.size.toLong(),
                    )
                    WGPUVertexBufferLayout.attributes(nativeBufferLayout, attributes)
                }

            val descriptor = WGPURenderPipelineDescriptor.allocate(scope)
            val vertexState = WGPURenderPipelineDescriptor.vertex(descriptor)
            val primitiveState = WGPURenderPipelineDescriptor.primitive(descriptor)
            val multisampleState = WGPURenderPipelineDescriptor.multisample(descriptor)

            WGPURenderPipelineDescriptor.label(
                descriptor,
                desc.label?.toNativeString(scope) ?: WGPU_NULL,
            )
            WGPURenderPipelineDescriptor.layout(descriptor, desc.layout.segment)

            WGPUVertexState.module(vertexState, desc.vertex.module.segment)
            WGPUVertexState.entryPoint(vertexState, desc.vertex.entryPoint.toNativeString(scope))
            WGPUVertexState.buffers(vertexState, buffers)
            WGPUVertexState.bufferCount(vertexState, desc.vertex.buffers.size.toLong())

            WGPUPrimitiveState.topology(primitiveState, desc.primitive.topology.nativeVal)
            WGPUPrimitiveState.stripIndexFormat(
                primitiveState,
                desc.primitive.stripIndexFormat?.nativeVal ?: WGPUIndexFormat_Undefined(),
            )
            WGPUPrimitiveState.frontFace(primitiveState, desc.primitive.frontFace.nativeVal)
            WGPUPrimitiveState.cullMode(primitiveState, desc.primitive.cullMode.nativeVal)

            desc.depthStencil?.let {
                val depthStencilState = WGPUDepthStencilState.allocate(scope)
                WGPUDepthStencilState.format(depthStencilState, it.format.nativeVal)
                WGPUDepthStencilState.depthWriteEnabled(
                    depthStencilState,
                    it.depthWriteEnabled.toInt(),
                )
                WGPUDepthStencilState.depthCompare(depthStencilState, it.depthCompare.nativeVal)
                WGPUDepthStencilState.stencilReadMask(depthStencilState, it.stencil.readMask)
                WGPUDepthStencilState.stencilWriteMask(depthStencilState, it.stencil.writeMask)
                WGPUDepthStencilState.depthBias(depthStencilState, it.bias.constant)
                WGPUDepthStencilState.depthBiasSlopeScale(depthStencilState, it.bias.slopeScale)
                WGPUDepthStencilState.depthBiasClamp(depthStencilState, it.bias.clamp)

                val stencilFront = WGPUDepthStencilState.stencilFront(depthStencilState)
                WGPUStencilFaceState.compare(stencilFront, it.stencil.front.compare.nativeVal)
                WGPUStencilFaceState.failOp(stencilFront, it.stencil.front.failOp.nativeVal)
                WGPUStencilFaceState.depthFailOp(
                    stencilFront,
                    it.stencil.front.depthFailOp.nativeVal,
                )
                WGPUStencilFaceState.passOp(stencilFront, it.stencil.front.passOp.nativeVal)

                val stencilBack = WGPUDepthStencilState.stencilBack(depthStencilState)
                WGPUStencilFaceState.compare(stencilBack, it.stencil.back.compare.nativeVal)
                WGPUStencilFaceState.failOp(stencilBack, it.stencil.back.failOp.nativeVal)
                WGPUStencilFaceState.depthFailOp(stencilBack, it.stencil.back.depthFailOp.nativeVal)
                WGPUStencilFaceState.passOp(stencilBack, it.stencil.back.passOp.nativeVal)

                WGPURenderPipelineDescriptor.depthStencil(descriptor, depthStencilState)
            }

            WGPUMultisampleState.count(multisampleState, desc.multisample.count)
            WGPUMultisampleState.mask(multisampleState, desc.multisample.mask)
            WGPUMultisampleState.alphaToCoverageEnabled(
                multisampleState,
                desc.multisample.alphaToCoverageEnabled.toInt(),
            )

            WGPURenderPipelineDescriptor.fragment(descriptor, fragDesc)

            RenderPipeline(wgpuDeviceCreateRenderPipeline(segment, descriptor))
        }
    }

    actual fun createComputePipeline(desc: ComputePipelineDescriptor): ComputePipeline {
        return Arena.ofConfined().use { scope ->
            val wgpuDesc = WGPUComputePipelineDescriptor.allocate(scope)

            val computeDesc = WGPUProgrammableStageDescriptor.allocate(scope)
            WGPUProgrammableStageDescriptor.entryPoint(
                computeDesc,
                desc.compute.entryPoint.toNativeString(scope),
            )
            WGPUProgrammableStageDescriptor.module(computeDesc, desc.compute.module.segment)

            WGPUComputePipelineDescriptor.layout(wgpuDesc, desc.layout.segment)
            WGPUComputePipelineDescriptor.compute(wgpuDesc, computeDesc)
            WGPUComputePipelineDescriptor.label(
                wgpuDesc,
                desc.label?.toNativeString(scope) ?: WGPU_NULL,
            )

            ComputePipeline(wgpuDeviceCreateComputePipeline(segment, wgpuDesc))
        }
    }

    actual fun createPipelineLayout(desc: PipelineLayoutDescriptor): PipelineLayout {
        return Arena.ofConfined().use { scope ->
            val wgpuDesc = WGPUPipelineLayoutDescriptor.allocate(scope)
            WGPUPipelineLayoutDescriptor.bindGroupLayouts(
                wgpuDesc,
                desc.segments.toNativeArray(scope),
            )
            WGPUPipelineLayoutDescriptor.bindGroupLayoutCount(wgpuDesc, desc.segments.size.toLong())
            WGPUPipelineLayoutDescriptor.label(
                wgpuDesc,
                desc.label?.toNativeString(scope) ?: WGPU_NULL,
            )
            PipelineLayout(wgpuDeviceCreatePipelineLayout(segment, wgpuDesc))
        }
    }

    actual fun createCommandEncoder(label: String?): CommandEncoder {
        return Arena.ofConfined().use { scope ->
            val descriptor = WGPUCommandEncoderDescriptor.allocate(scope)
            WGPUCommandEncoderDescriptor.label(
                descriptor,
                label?.toNativeString(scope) ?: WGPU_NULL,
            )
            CommandEncoder(wgpuDeviceCreateCommandEncoder(segment, descriptor))
        }
    }

    actual fun createBuffer(desc: BufferDescriptor): GPUBuffer {
        return Arena.ofConfined().use { scope ->
            val descriptor = WGPUBufferDescriptor.allocate(scope)
            WGPUBufferDescriptor.nextInChain(descriptor, WGPU_NULL)
            WGPUBufferDescriptor.usage(descriptor, desc.usage.usageFlag)
            WGPUBufferDescriptor.size(descriptor, desc.size)
            WGPUBufferDescriptor.mappedAtCreation(descriptor, desc.mappedAtCreation.toInt())
            WGPUBufferDescriptor.label(descriptor, desc.label.toNativeString(scope))
            GPUBuffer(wgpuDeviceCreateBuffer(segment, descriptor).asSlice(0, desc.size), desc.size)
        }
    }

    actual fun createBindGroupLayout(desc: BindGroupLayoutDescriptor): BindGroupLayout {
        return Arena.ofConfined().use { scope ->
            val descriptor = WGPUBindGroupLayoutDescriptor.allocate(scope)
            WGPUBindGroupLayoutDescriptor.label(
                descriptor,
                desc.label?.toNativeString(scope) ?: WGPU_NULL,
            )
            val entries =
                desc.entries.mapToNativeEntries(
                    scope,
                    WGPUBindGroupLayoutEntry.sizeof(),
                    WGPUBindGroupLayoutEntry::allocateArray,
                ) { entry, nativeEntry ->
                    WGPUBindGroupLayoutEntry.binding(nativeEntry, entry.binding)
                    WGPUBindGroupLayoutEntry.visibility(nativeEntry, entry.visibility.usageFlag)

                    val bufferBinding = WGPUBindGroupLayoutEntry.buffer(nativeEntry)
                    val samplerBinding = WGPUBindGroupLayoutEntry.sampler(nativeEntry)
                    val textureBinding = WGPUBindGroupLayoutEntry.texture(nativeEntry)
                    val storageTextureBinding = WGPUBindGroupLayoutEntry.storageTexture(nativeEntry)

                    WGPUBufferBindingLayout.type(bufferBinding, WGPUBufferBindingType_Undefined())
                    WGPUSamplerBindingLayout.type(
                        samplerBinding,
                        WGPUSamplerBindingType_Undefined(),
                    )
                    WGPUTextureBindingLayout.sampleType(
                        textureBinding,
                        WGPUTextureSampleType_Undefined(),
                    )
                    WGPUStorageTextureBindingLayout.access(
                        storageTextureBinding,
                        WGPUStorageTextureAccess_Undefined(),
                    )

                    entry.bindingLayout.intoNative(
                        bufferBinding,
                        samplerBinding,
                        textureBinding,
                        storageTextureBinding,
                    )
                }
            WGPUBindGroupLayoutDescriptor.entries(descriptor, entries)
            WGPUBindGroupLayoutDescriptor.entryCount(descriptor, desc.entries.size.toLong())
            BindGroupLayout(wgpuDeviceCreateBindGroupLayout(segment, descriptor))
        }
    }

    actual fun createBindGroup(desc: BindGroupDescriptor): BindGroup {
        return Arena.ofConfined().use { scope ->
            val descriptor = WGPUBindGroupDescriptor.allocate(scope)
            val entries =
                desc.entries.mapToNativeEntries(
                    scope,
                    WGPUBindGroupEntry.sizeof(),
                    WGPUBindGroupEntry::allocateArray,
                ) { entry, nativeEntry ->
                    WGPUBindGroupEntry.binding(nativeEntry, entry.binding)
                    entry.resource.intoBindingResource(nativeEntry)
                }
            WGPUBindGroupDescriptor.label(
                descriptor,
                desc.label?.toNativeString(scope) ?: WGPU_NULL,
            )
            WGPUBindGroupDescriptor.layout(descriptor, desc.layout.segment)
            WGPUBindGroupDescriptor.entries(descriptor, entries)
            WGPUBindGroupDescriptor.entryCount(descriptor, desc.entries.size.toLong())
            BindGroup(wgpuDeviceCreateBindGroup(segment, descriptor))
        }
    }

    actual fun createSampler(desc: SamplerDescriptor): Sampler {
        return Arena.ofConfined().use { scope ->
            val descriptor = WGPUSamplerDescriptor.allocate(scope)

            WGPUSamplerDescriptor.label(descriptor, desc.label?.toNativeString(scope) ?: WGPU_NULL)
            WGPUSamplerDescriptor.addressModeU(descriptor, desc.addressModeU.nativeVal)
            WGPUSamplerDescriptor.addressModeV(descriptor, desc.addressModeV.nativeVal)
            WGPUSamplerDescriptor.addressModeW(descriptor, desc.addressModeW.nativeVal)
            WGPUSamplerDescriptor.magFilter(descriptor, desc.magFilter.nativeVal)
            WGPUSamplerDescriptor.minFilter(descriptor, desc.minFilter.nativeVal)
            WGPUSamplerDescriptor.mipmapFilter(descriptor, desc.mipmapFilter.nativeVal)
            WGPUSamplerDescriptor.lodMinClamp(descriptor, desc.lodMinClamp)
            WGPUSamplerDescriptor.lodMaxClamp(descriptor, desc.lodMaxClamp)
            WGPUSamplerDescriptor.compare(
                descriptor,
                desc.compare?.nativeVal ?: WGPUCompareFunction_Undefined(),
            )
            WGPUSamplerDescriptor.maxAnisotropy(descriptor, desc.maxAnisotropy)

            Sampler(wgpuDeviceCreateSampler(segment, descriptor))
        }
    }

    actual fun createTexture(desc: TextureDescriptor): WebGPUTexture {
        return Arena.ofConfined().use { scope ->
            val descriptor = WGPUTextureDescriptor.allocate(scope)
            val size = WGPUTextureDescriptor.size(descriptor)

            WGPUTextureDescriptor.label(descriptor, desc.label?.toNativeString(scope) ?: WGPU_NULL)
            WGPUTextureDescriptor.usage(descriptor, desc.usage.usageFlag)
            WGPUTextureDescriptor.dimension(descriptor, desc.dimension.nativeVal)
            WGPUTextureDescriptor.format(descriptor, desc.format.nativeVal)
            WGPUTextureDescriptor.mipLevelCount(descriptor, desc.mipLevelCount)
            WGPUTextureDescriptor.sampleCount(descriptor, desc.sampleCount)
            WGPUExtent3D.width(size, desc.size.width)
            WGPUExtent3D.height(size, desc.size.height)
            WGPUExtent3D.depthOrArrayLayers(size, desc.size.depth)

            val textureSize =
                (desc.size.width * desc.size.height * desc.size.depth * desc.format.bytes).toLong()
            WebGPUTexture(wgpuDeviceCreateTexture(segment, descriptor), textureSize)
        }
    }

    actual fun createGPUShortBuffer(
        label: String,
        data: ShortArray,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.size.toLong() * Short.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange().putShort(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUFloatBuffer(
        label: String,
        data: FloatArray,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.size.toLong() * Float.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange().putFloat(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUIntBuffer(label: String, data: IntArray, usage: BufferUsage): GPUBuffer {
        val buffer =
            createBuffer(BufferDescriptor(label, data.size.toLong() * Int.SIZE_BYTES, usage, true))
        buffer.getMappedRange().putInt(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUByteBuffer(label: String, data: ByteArray, usage: BufferUsage): GPUBuffer {
        val buffer = createBuffer(BufferDescriptor(label, data.size.toLong(), usage, true))
        buffer.getMappedRange().putByte(data)
        buffer.unmap()

        return buffer
    }

    actual override fun release() {
        wgpuDeviceRelease(segment)
    }

    override fun toString(): String {
        return "Device"
    }
}

actual class Adapter(var segment: MemorySegment) : Releasable {
    /** The features which can be used to create devices on this adapter. */
    actual val features: List<Feature> by lazy {
        val list = mutableListOf<Feature>()
        Feature.entries.forEach {
            val result = wgpuAdapterHasFeature(segment, it.nativeVal)
            if (result == 1) {
                list += it
            }
        }
        list.toList()
    }

    /**
     * The best limits which can be used to create devices on this adapter. Each adapter limit must
     * be the same or better than its default value in supported limits.
     */
    actual val limits: Limits by lazy {
        Arena.ofConfined().use { scope ->
            val supported = WGPUSupportedLimits.allocate(scope)
            wgpuAdapterGetLimits(segment, supported)
            val desc = WGPUSupportedLimits.limits(supported)

            Limits(
                maxTextureDimension1D = WGPULimits.maxTextureDimension1D(desc),
                maxTextureDimension2D = WGPULimits.maxTextureDimension2D(desc),
                maxTextureDimension3D = WGPULimits.maxTextureDimension3D(desc),
                maxTextureArrayLayers = WGPULimits.maxTextureArrayLayers(desc),
                maxBindGroups = WGPULimits.maxBindGroups(desc),
                maxBindGroupsPlusVertexBuffers = WGPULimits.maxBindGroupsPlusVertexBuffers(desc),
                maxBindingsPerBindGroup = WGPULimits.maxBindingsPerBindGroup(desc),
                maxDynamicUniformBuffersPerPipelineLayout =
                    WGPULimits.maxDynamicUniformBuffersPerPipelineLayout(desc),
                maxDynamicStorageBuffersPerPipelineLayout =
                    WGPULimits.maxDynamicStorageBuffersPerPipelineLayout(desc),
                maxSampledTexturesPerShaderStage =
                    WGPULimits.maxSampledTexturesPerShaderStage(desc),
                maxSamplersPerShaderStage = WGPULimits.maxSamplersPerShaderStage(desc),
                maxStorageBuffersPerShaderStage = WGPULimits.maxStorageBuffersPerShaderStage(desc),
                maxStorageTexturesPerShaderStage =
                    WGPULimits.maxStorageTexturesPerShaderStage(desc),
                maxUniformBuffersPerShaderStage = WGPULimits.maxUniformBuffersPerShaderStage(desc),
                maxUniformBufferBindingSize = WGPULimits.maxUniformBufferBindingSize(desc),
                maxStorageBufferBindingSize = WGPULimits.maxStorageBufferBindingSize(desc),
                minUniformBufferOffsetAlignment = WGPULimits.minUniformBufferOffsetAlignment(desc),
                minStorageBufferOffsetAlignment = WGPULimits.minStorageBufferOffsetAlignment(desc),
                maxVertexBuffers = WGPULimits.maxVertexBuffers(desc),
                maxBufferSize = WGPULimits.maxBufferSize(desc),
                maxVertexAttributes = WGPULimits.maxVertexAttributes(desc),
                maxVertexBufferArrayStride = WGPULimits.maxVertexBufferArrayStride(desc),
                maxInterStageShaderComponents = WGPULimits.maxInterStageShaderComponents(desc),
                maxInterStageShaderVariables = WGPULimits.maxInterStageShaderVariables(desc),
                maxColorAttachments = WGPULimits.maxColorAttachments(desc),
                maxColorAttachmentBytesPerSample =
                    WGPULimits.maxColorAttachmentBytesPerSample(desc),
                maxComputeWorkgroupStorageSize = WGPULimits.maxComputeWorkgroupStorageSize(desc),
                maxComputeInvocationsPerWorkgroup =
                    WGPULimits.maxComputeInvocationsPerWorkgroup(desc),
                maxComputeWorkgroupSizeX = WGPULimits.maxComputeWorkgroupSizeX(desc),
                maxComputeWorkgroupSizeY = WGPULimits.maxComputeWorkgroupSizeY(desc),
                maxComputeWorkgroupSizeZ = WGPULimits.maxComputeWorkgroupSizeZ(desc),
                maxComputeWorkgroupsPerDimension = WGPULimits.maxComputeWorkgroupsPerDimension(desc),
            )
        }
    }

    actual suspend fun requestDevice(descriptor: DeviceDescriptor?): Device {
        val output = atomic(WGPU_NULL)
        Arena.ofConfined().use { scope ->
            val desc = WGPUDeviceDescriptor.allocate(scope)
            val deviceExtras = WGPUDeviceExtras.allocate(scope)
            val chainedStruct = WGPUDeviceExtras.chain(deviceExtras)
            val callback =
                WGPUAdapterRequestDeviceCallback.allocate(
                    { status, device, message, _ ->
                        if (status == WGPURequestAdapterStatus_Success()) {
                            output.update { device }
                        } else {
                            logger.log(Logger.Level.ERROR) {
                                "requestDevice status=$status, message=${message.getString(0)}"
                            }
                        }
                    },
                    scope,
                )

            WGPUChainedStruct.sType(chainedStruct, WGPUSType_DeviceExtras())
            WGPUDeviceDescriptor.nextInChain(desc, deviceExtras)
            if (descriptor != null) {
                descriptor.label?.let { WGPUDeviceDescriptor.label(desc, it.toNativeString(scope)) }
                descriptor.requiredFeatures?.let {
                    val nativeArray = scope.allocateFrom(ValueLayout.JAVA_INT, it.size)
                    it.forEachIndexed { index, jvmEntry ->
                        val nativeEntry = nativeArray.asSlice((Int.SIZE_BYTES * index).toLong())

                        nativeEntry.set(ValueLayout.JAVA_INT, 0L, jvmEntry.nativeVal)
                    }
                    WGPUDeviceDescriptor.requiredFeatureCount(desc, it.size.toLong())
                    WGPUDeviceDescriptor.requiredFeatures(desc, nativeArray)
                }
                descriptor.requiredLimits?.let { requiredLimits ->
                    val nativeLimits = WGPULimits.allocate(scope)
                    requiredLimits.maxTextureDimension1D?.let {
                        WGPULimits.maxTextureDimension1D(nativeLimits, it)
                    }
                    requiredLimits.maxTextureDimension2D?.let {
                        WGPULimits.maxTextureDimension2D(nativeLimits, it)
                    }
                    requiredLimits.maxTextureDimension3D?.let {
                        WGPULimits.maxTextureDimension3D(nativeLimits, it)
                    }
                    requiredLimits.maxTextureArrayLayers?.let {
                        WGPULimits.maxTextureArrayLayers(nativeLimits, it)
                    }
                    requiredLimits.maxBindGroups?.let { WGPULimits.maxBindGroups(nativeLimits, it) }
                    requiredLimits.maxBindGroupsPlusVertexBuffers?.let {
                        WGPULimits.maxBindGroupsPlusVertexBuffers(nativeLimits, it)
                    }
                    requiredLimits.maxBindingsPerBindGroup?.let {
                        WGPULimits.maxBindingsPerBindGroup(nativeLimits, it)
                    }
                    requiredLimits.maxDynamicUniformBuffersPerPipelineLayout?.let {
                        WGPULimits.maxDynamicUniformBuffersPerPipelineLayout(nativeLimits, it)
                    }
                    requiredLimits.maxDynamicStorageBuffersPerPipelineLayout?.let {
                        WGPULimits.maxDynamicStorageBuffersPerPipelineLayout(nativeLimits, it)
                    }
                    requiredLimits.maxSampledTexturesPerShaderStage?.let {
                        WGPULimits.maxSampledTexturesPerShaderStage(nativeLimits, it)
                    }
                    requiredLimits.maxSamplersPerShaderStage?.let {
                        WGPULimits.maxSamplersPerShaderStage(nativeLimits, it)
                    }
                    requiredLimits.maxStorageBuffersPerShaderStage?.let {
                        WGPULimits.maxStorageBuffersPerShaderStage(nativeLimits, it)
                    }
                    requiredLimits.maxStorageTexturesPerShaderStage?.let {
                        WGPULimits.maxStorageTexturesPerShaderStage(nativeLimits, it)
                    }
                    requiredLimits.maxUniformBuffersPerShaderStage?.let {
                        WGPULimits.maxUniformBuffersPerShaderStage(nativeLimits, it)
                    }
                    requiredLimits.maxUniformBufferBindingSize?.let {
                        WGPULimits.maxUniformBufferBindingSize(nativeLimits, it)
                    }
                    requiredLimits.maxStorageBufferBindingSize?.let {
                        WGPULimits.maxStorageBufferBindingSize(nativeLimits, it)
                    }
                    requiredLimits.minUniformBufferOffsetAlignment?.let {
                        WGPULimits.minUniformBufferOffsetAlignment(nativeLimits, it)
                    }
                    requiredLimits.minStorageBufferOffsetAlignment?.let {
                        WGPULimits.minStorageBufferOffsetAlignment(nativeLimits, it)
                    }
                    requiredLimits.maxVertexBuffers?.let {
                        WGPULimits.maxVertexBuffers(nativeLimits, it)
                    }
                    requiredLimits.maxBufferSize?.let { WGPULimits.maxBufferSize(nativeLimits, it) }
                    requiredLimits.maxVertexAttributes?.let {
                        WGPULimits.maxVertexAttributes(nativeLimits, it)
                    }
                    requiredLimits.maxVertexBufferArrayStride?.let {
                        WGPULimits.maxVertexBufferArrayStride(nativeLimits, it)
                    }
                    val nativeRequiredLimits = WGPURequiredLimits.allocate(scope)
                    WGPURequiredLimits.limits(nativeRequiredLimits, nativeLimits)
                }
            }

            wgpuAdapterRequestDevice(segment, desc, callback, WGPU_NULL)
        }
        return Device(output.value)
    }

    actual override fun release() {
        wgpuAdapterRelease(segment)
    }

    override fun toString(): String {
        return "Adapter"
    }

    companion object {
        private val logger = Logger<Adapter>()
    }
}

actual class Queue(val segment: MemorySegment) : Releasable {

    actual fun submit(vararg cmdBuffers: CommandBuffer) {
        Arena.ofConfined().use { scope ->
            wgpuQueueSubmit(
                segment,
                cmdBuffers.size.toLong(),
                cmdBuffers.map { it.segment }.toNativeArray(scope),
            )
        }
    }

    actual fun writeTexture(
        data: ByteBuffer,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long,
    ) {
        data as ByteBufferImpl
        Arena.ofConfined().use { scope ->
            wgpuQueueWriteTexture(
                segment,
                destination.toNative(scope),
                data.segment,
                size,
                layout.toNative(scope),
                copySize.toNative(scope),
            )
        }
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ShortBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as ShortBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset,
            if (dataOffset > 0) data.segment.asSlice(dataOffset) else data.segment,
            size * Short.SIZE_BYTES,
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: IntBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as IntBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset,
            if (dataOffset > 0) data.segment.asSlice(dataOffset) else data.segment,
            size * Int.SIZE_BYTES,
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: FloatBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as FloatBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset,
            if (dataOffset > 0) data.segment.asSlice(dataOffset) else data.segment,
            size * Float.SIZE_BYTES,
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ByteBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as ByteBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset,
            if (dataOffset > 0) data.segment.asSlice(dataOffset) else data.segment,
            size,
        )
    }

    actual fun writeTexture(
        data: ByteArray,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long,
    ) {
        Arena.ofConfined().use { scope ->
            wgpuQueueWriteTexture(
                segment,
                destination.toNative(scope),
                scope.allocateFrom(ValueLayout.JAVA_BYTE, *data),
                size,
                layout.toNative(scope),
                copySize.toNative(scope),
            )
        }
    }

    actual override fun release() {
        wgpuQueueRelease(segment)
    }

    override fun toString(): String {
        return "Queue"
    }
}

actual class ShaderModule(val segment: MemorySegment) : Releasable {
    actual override fun release() {
        wgpuShaderModuleRelease(segment)
    }

    override fun toString(): String {
        return "ShaderModule"
    }
}

actual class Surface(val segment: MemorySegment) : Releasable {

    actual fun configure(configuration: SurfaceConfiguration) {
        Arena.ofConfined().use { scope ->
            val desc = WGPUSurfaceConfiguration.allocate(scope)
            WGPUSurfaceConfiguration.device(desc, configuration.device.segment)
            WGPUSurfaceConfiguration.usage(desc, configuration.usage.usageFlag)
            WGPUSurfaceConfiguration.format(desc, configuration.format.nativeVal)
            WGPUSurfaceConfiguration.presentMode(desc, configuration.presentMode.nativeVal)
            WGPUSurfaceConfiguration.alphaMode(desc, configuration.alphaMode.nativeVal)
            WGPUSurfaceConfiguration.width(desc, configuration.width)
            WGPUSurfaceConfiguration.height(desc, configuration.height)
            wgpuSurfaceConfigure(segment, desc)
        }
    }

    actual fun getCurrentTexture(): SurfaceTexture {
        return Arena.ofConfined().use { scope ->
            val surfaceTexture: MemorySegment = WGPUSurfaceTexture.allocate(scope)
            wgpuSurfaceGetCurrentTexture(segment, surfaceTexture)
            val texture =
                WGPUSurfaceTexture.texture(surfaceTexture).let {
                    if (it == WGPU_NULL) null else WebGPUTexture(it, it.byteSize())
                }
            val status =
                WGPUSurfaceTexture.status(surfaceTexture).let {
                    TextureStatus.from(it) ?: error("Invalid texture status: $it")
                }
            SurfaceTexture(surfaceTexture, texture, status)
        }
    }

    actual fun present() {
        wgpuSurfacePresent(segment)
    }

    actual fun getCapabilities(adapter: Adapter): SurfaceCapabilities {
        return Arena.ofConfined().use { scope ->
            val surfaceCapabilities = WGPUSurfaceCapabilities.allocate(scope)
            wgpuSurfaceGetCapabilities(segment, adapter.segment, surfaceCapabilities)
            val formats =
                WGPUSurfaceCapabilities.formats(surfaceCapabilities)
                    .mapFromIntUntilNull { TextureFormat.from(it) }
                    .distinct()
            val alphaModes =
                WGPUSurfaceCapabilities.alphaModes(surfaceCapabilities)
                    .mapFromIntUntilNull { AlphaMode.from(it) }
                    .distinct()
            SurfaceCapabilities(surfaceCapabilities, formats, alphaModes)
        }
    }

    actual fun getPreferredFormat(adapter: Adapter): TextureFormat {
        return getCapabilities(adapter).formats[0]
    }

    actual override fun release() {
        wgpuSurfaceRelease(segment)
    }
}

actual class WebGPUTexture(val segment: MemorySegment, val size: Long) : Releasable {

    private val info = TextureResourceInfo(this, size)

    actual fun createView(desc: TextureViewDescriptor?): TextureView {
        val descSeg =
            if (desc != null) {
                Arena.ofConfined().use { scope ->
                    WGPUTextureViewDescriptor.allocate(scope).also {
                        WGPUTextureViewDescriptor.label(
                            it,
                            desc.label?.toNativeString(scope) ?: WGPU_NULL,
                        )
                        WGPUTextureViewDescriptor.format(it, desc.format.nativeVal)
                        WGPUTextureViewDescriptor.dimension(it, desc.dimension.nativeVal)
                        WGPUTextureViewDescriptor.aspect(it, desc.aspect.nativeVal)
                        WGPUTextureViewDescriptor.baseMipLevel(it, desc.baseMipLevel)
                        WGPUTextureViewDescriptor.mipLevelCount(it, desc.mipLevelCount)
                        WGPUTextureViewDescriptor.baseArrayLayer(it, desc.baseArrayLayer)
                        WGPUTextureViewDescriptor.arrayLayerCount(it, desc.arrayLayerCount)
                    }
                }
            } else {
                WGPU_NULL
            }
        return TextureView(wgpuTextureCreateView(segment, descSeg))
    }

    actual override fun release() {
        wgpuTextureRelease(segment)
        info.delete()
    }

    actual fun destroy() {
        wgpuTextureDestroy(segment)
        info.delete()
    }
}

actual class TextureView(val segment: MemorySegment) : IntoBindingResource {

    override fun intoBindingResource(entry: MemorySegment) {
        WGPUBindGroupEntry.textureView(entry, segment)
    }

    actual fun release() {
        wgpuTextureViewRelease(segment)
    }
}

actual class GPUBuffer(val segment: MemorySegment, actual val size: Long) : Releasable {

    private val info = BufferResourceInfo(this, size)

    actual fun getMappedRange(offset: Long, size: Long): ByteBuffer {
        val mappedRange = wgpuBufferGetMappedRange(segment, offset, size).asSlice(offset, size)
        return ByteBufferImpl(mappedRange.byteSize().toInt(), segment = mappedRange)
    }

    actual fun getMappedRange(): ByteBuffer = getMappedRange(0, size)

    actual fun unmap() {
        wgpuBufferUnmap(segment)
    }

    actual override fun release() {
        wgpuBufferRelease(segment)
    }

    actual fun destroy() {
        wgpuBufferDestroy(segment)
        info.delete()
    }
}

actual class Sampler(val segment: MemorySegment) : IntoBindingResource, Releasable {

    override fun intoBindingResource(entry: MemorySegment) {
        WGPUBindGroupEntry.sampler(entry, segment)
    }

    actual override fun release() {
        wgpuSamplerRelease(segment)
    }
}

fun Extent3D.toNative(scope: SegmentAllocator): MemorySegment {
    val native = WGPUExtent3D.allocate(scope)

    WGPUExtent3D.width(native, width)
    WGPUExtent3D.height(native, height)
    WGPUExtent3D.depthOrArrayLayers(native, depth)

    return native
}

actual class SurfaceCapabilities(
    val segment: MemorySegment,
    actual val formats: List<TextureFormat>,
    actual val alphaModes: List<AlphaMode>,
) {}

actual class SurfaceTexture(
    val segment: MemorySegment,
    actual val texture: WebGPUTexture?,
    actual val status: TextureStatus,
)
