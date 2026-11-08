package com.littlekt.graphics.g3d.ibl

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.file.hdr.HdrImage
import com.littlekt.graphics.webgpu.*
import kotlin.math.log2
import kotlin.math.max

/**
 * An environment map used for image based lighting (IBL) and skyboxes. An equirectangular
 * [HdrImage] is baked, on the GPU, into:
 * * [cubeView]: the environment as a cubemap with mips. Used by a [Skybox].
 * * [irradianceView]: the diffuse irradiance of the environment.
 * * [prefilteredView]: the specular reflections of the environment, blurrier per mip as the
 *   roughness increases.
 * * [brdfView]: the BRDF lookup texture.
 *
 * Pass it to a [com.littlekt.graphics.g3d.PBREnvironment] to light PBR materials with it.
 *
 * @param image the equirectangular HDR image of the environment.
 * @param size the width & height of each face of [cubeView].
 * @param irradianceSize the width & height of each face of [irradianceView].
 * @param prefilteredSize the width & height of each face of the first mip of [prefilteredView].
 * @param prefilteredMipLevels the number of roughness levels, as mips, of [prefilteredView].
 * @author Colton Daily
 * @date 11/8/2026
 */
class EnvironmentMap(
    val device: Device,
    image: HdrImage,
    val size: Int = 512,
    val irradianceSize: Int = 32,
    val prefilteredSize: Int = 128,
    val prefilteredMipLevels: Int = 5,
) : Releasable {

    /** Multiplier of the image based lighting. */
    var intensity: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            writeUniform()
        }

    private val cubeMipLevels = log2(size.toFloat()).toInt() + 1

    private val cubeTexture = createCube(size, cubeMipLevels, "environment map cube")
    private val irradianceTexture = createCube(irradianceSize, 1, "environment map irradiance")
    private val prefilteredTexture =
        createCube(prefilteredSize, prefilteredMipLevels, "environment map prefiltered")
    private val brdfTexture =
        device.createTexture(
            TextureDescriptor(
                Extent3D(BRDF_SIZE, BRDF_SIZE, 1),
                1,
                1,
                TextureDimension.D2,
                BRDF_FORMAT,
                TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
                "environment map brdf",
            )
        )

    /** The environment cubemap with mips. */
    val cubeView: TextureView = createCubeView(cubeTexture, cubeMipLevels)

    /** The diffuse irradiance cubemap. */
    val irradianceView: TextureView = createCubeView(irradianceTexture, 1)

    /** The prefiltered specular cubemap. Each mip is a higher roughness. */
    val prefilteredView: TextureView = createCubeView(prefilteredTexture, prefilteredMipLevels)

    /** The BRDF lookup texture. */
    val brdfView: TextureView = brdfTexture.createView()

    /** The linear sampler used to sample each view. */
    val sampler: Sampler =
        device.createSampler(
            SamplerDescriptor(
                magFilter = FilterMode.LINEAR,
                minFilter = FilterMode.LINEAR,
                mipmapFilter = FilterMode.LINEAR,
                label = "environment map sampler",
            )
        )

    private val uniformData = FloatBuffer(4)
    private val uniformBuffer =
        device.createGPUFloatBuffer(
            "environment map uniform",
            uniformData,
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] holding the [intensity] & prefiltered mip count. */
    val uniformBufferBinding = BufferBinding(uniformBuffer)

    init {
        bake(image)
        writeUniform()
    }

    private fun writeUniform() {
        uniformData[0] = intensity
        uniformData[1] = (prefilteredMipLevels - 1).toFloat()
        uniformData[2] = 0f
        uniformData[3] = 0f
        device.queue.writeBuffer(uniformBuffer, uniformData)
    }

    private fun createCube(size: Int, mipLevels: Int, label: String) =
        device.createTexture(
            TextureDescriptor(
                Extent3D(size, size, 6),
                mipLevels,
                1,
                TextureDimension.D2,
                FORMAT,
                TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
                label,
            )
        )

    private fun createCubeView(texture: WebGPUTexture, mipLevels: Int) =
        texture.createView(
            TextureViewDescriptor(
                FORMAT,
                TextureViewDimension.CUBE,
                mipLevelCount = mipLevels,
                arrayLayerCount = 6,
            )
        )

    private fun bake(image: HdrImage) {
        val temp = mutableListOf<Releasable>()
        val tempViews = mutableListOf<TextureView>()
        fun <T : Releasable> T.temp(): T = also { temp += it }
        fun TextureView.temp(): TextureView = also { tempViews += it }

        val equirect =
            device
                .createTexture(
                    TextureDescriptor(
                        Extent3D(image.width, image.height, 1),
                        1,
                        1,
                        TextureDimension.D2,
                        FORMAT,
                        TextureUsage.TEXTURE or TextureUsage.COPY_DST,
                        "environment map equirect",
                    )
                )
                .temp()
        device.queue.writeTexture(
            image.toRgba16Float(),
            TextureCopyView(equirect),
            TextureDataLayout(image.width * 8, image.height),
            Extent3D(image.width, image.height, 1),
        )
        val equirectView = equirect.createView().temp()
        val bakeSampler =
            device
                .createSampler(
                    SamplerDescriptor(
                        addressModeU = AddressMode.REPEAT,
                        magFilter = FilterMode.LINEAR,
                        minFilter = FilterMode.LINEAR,
                        mipmapFilter = FilterMode.LINEAR,
                    )
                )
                .temp()

        val layout2d =
            device
                .createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                            BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        )
                    )
                )
                .temp()
        val layoutCube =
            device
                .createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(
                                0,
                                ShaderStage.FRAGMENT,
                                TextureBindingLayout(viewDimension = TextureViewDimension.CUBE),
                            ),
                            BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        )
                    )
                )
                .temp()
        val pipelineLayout2d =
            device.createPipelineLayout(PipelineLayoutDescriptor(layout2d)).temp()
        val pipelineLayoutCube =
            device.createPipelineLayout(PipelineLayoutDescriptor(layoutCube)).temp()
        val pipelineLayoutEmpty = device.createPipelineLayout(PipelineLayoutDescriptor()).temp()

        val module2d = device.createShaderModule(shader2d()).temp()
        val moduleCube = device.createShaderModule(shaderCube()).temp()
        val moduleBrdf = device.createShaderModule(shaderBrdf()).temp()

        fun pipeline(
            layout: PipelineLayout,
            module: ShaderModule,
            entryPoint: String,
            format: TextureFormat,
        ) =
            device
                .createRenderPipeline(
                    RenderPipelineDescriptor(
                        layout,
                        vertex = VertexState(module = module, entryPoint = "vs_main"),
                        fragment =
                            FragmentState(
                                module = module,
                                entryPoint = entryPoint,
                                target =
                                    ColorTargetState(
                                        format = format,
                                        blendState = null,
                                        writeMask = ColorWriteMask.ALL,
                                    ),
                            ),
                        primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                        label = "environment map $entryPoint pipeline",
                    )
                )
                .temp()

        val equirectPipeline = pipeline(pipelineLayout2d, module2d, "fs_equirect", FORMAT)
        val downsamplePipeline = pipeline(pipelineLayout2d, module2d, "fs_downsample", FORMAT)
        val irradiancePipeline = pipeline(pipelineLayoutCube, moduleCube, "fs_irradiance", FORMAT)
        val prefilterPipeline = pipeline(pipelineLayoutCube, moduleCube, "fs_prefilter", FORMAT)
        val brdfPipeline = pipeline(pipelineLayoutEmpty, moduleBrdf, "fs_brdf", BRDF_FORMAT)

        fun bindGroup(layout: BindGroupLayout, view: TextureView) =
            device
                .createBindGroup(
                    BindGroupDescriptor(
                        layout,
                        listOf(BindGroupEntry(0, view), BindGroupEntry(1, bakeSampler)),
                    )
                )
                .temp()

        fun faceView(texture: WebGPUTexture, mip: Int, face: Int) =
            texture
                .createView(
                    TextureViewDescriptor(
                        FORMAT,
                        TextureViewDimension.D2,
                        baseMipLevel = mip,
                        baseArrayLayer = face,
                    )
                )
                .temp()

        val encoder = device.createCommandEncoder("environment map bake")
        fun draw(target: TextureView, pipeline: RenderPipeline, bindGroup: BindGroup?, index: Int) {
            val pass =
                encoder.beginRenderPass(
                    RenderPassDescriptor(
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view = target,
                                loadOp = LoadOp.CLEAR,
                                storeOp = StoreOp.STORE,
                            )
                        ),
                        label = "environment map bake pass",
                    )
                )
            pass.setPipeline(pipeline)
            if (bindGroup != null) pass.setBindGroup(0, bindGroup)
            pass.draw(3, 1, firstInstance = index)
            pass.end()
            pass.release()
        }

        val equirectGroup = bindGroup(layout2d, equirectView)
        for (face in 0 until 6) {
            draw(faceView(cubeTexture, 0, face), equirectPipeline, equirectGroup, face)
        }
        for (mip in 1 until cubeMipLevels) {
            for (face in 0 until 6) {
                val source = bindGroup(layout2d, faceView(cubeTexture, mip - 1, face))
                draw(faceView(cubeTexture, mip, face), downsamplePipeline, source, face)
            }
        }
        val cubeGroup = bindGroup(layoutCube, cubeView)
        for (face in 0 until 6) {
            draw(faceView(irradianceTexture, 0, face), irradiancePipeline, cubeGroup, face)
        }
        for (mip in 0 until prefilteredMipLevels) {
            for (face in 0 until 6) {
                draw(
                    faceView(prefilteredTexture, mip, face),
                    prefilterPipeline,
                    cubeGroup,
                    mip * 6 + face,
                )
            }
        }
        draw(brdfView, brdfPipeline, null, 0)

        val commandBuffer = encoder.finish()
        device.queue.submit(commandBuffer)
        commandBuffer.release()
        encoder.release()
        tempViews.forEach { it.release() }
        temp.asReversed().forEach { it.release() }
    }

    private fun shader2d() =
        """
        $COMMON
        @group(0) @binding(0) var source: texture_2d<f32>;
        @group(0) @binding(1) var source_sampler: sampler;

        @fragment
        fn fs_equirect(input: VertexOutput) -> @location(0) vec4f {
            let dir = face_direction(input.index, input.uv);
            let uv = vec2f(atan2(dir.z, dir.x) / (2.0 * PI) + 0.5, acos(clamp(dir.y, -1.0, 1.0)) / PI);
            return vec4f(textureSampleLevel(source, source_sampler, uv, 0.0).rgb, 1.0);
        }

        @fragment
        fn fs_downsample(input: VertexOutput) -> @location(0) vec4f {
            return vec4f(textureSampleLevel(source, source_sampler, input.uv, 0.0).rgb, 1.0);
        }
        """
            .trimIndent()

    private fun shaderCube(): String {
        val irradianceLod = max(0f, log2(size / 16f))
        val prefilterBaseLod = max(0f, log2(size.toFloat() / prefilteredSize))
        return """
        $COMMON
        $SAMPLING
        @group(0) @binding(0) var source: texture_cube<f32>;
        @group(0) @binding(1) var source_sampler: sampler;

        @fragment
        fn fs_irradiance(input: VertexOutput) -> @location(0) vec4f {
            let n = face_direction(input.index, input.uv);
            var up = vec3f(0.0, 1.0, 0.0);
            if (abs(n.y) > 0.999) {
                up = vec3f(0.0, 0.0, 1.0);
            }
            let right = normalize(cross(up, n));
            up = cross(n, right);
            var irradiance = vec3f(0.0);
            var count = 0.0;
            for (var phi = 0.0; phi < 2.0 * PI; phi += $IRRADIANCE_DELTA) {
                for (var theta = 0.0; theta < 0.5 * PI; theta += $IRRADIANCE_DELTA) {
                    let t = vec3f(sin(theta) * cos(phi), sin(theta) * sin(phi), cos(theta));
                    let dir = t.x * right + t.y * up + t.z * n;
                    irradiance += textureSampleLevel(source, source_sampler, dir, $irradianceLod).rgb * cos(theta) * sin(theta);
                    count += 1.0;
                }
            }
            return vec4f(PI * irradiance / count, 1.0);
        }

        @fragment
        fn fs_prefilter(input: VertexOutput) -> @location(0) vec4f {
            let face = input.index % 6u;
            let mip = input.index / 6u;
            let roughness = f32(mip) / ${max(1, prefilteredMipLevels - 1)}.0;
            let n = face_direction(face, input.uv);
            if (roughness == 0.0) {
                return vec4f(textureSampleLevel(source, source_sampler, n, $prefilterBaseLod).rgb, 1.0);
            }
            let texel_solid_angle = 4.0 * PI / (6.0 * $size.0 * $size.0);
            var color = vec3f(0.0);
            var weight = 0.0;
            for (var i = 0u; i < ${PREFILTER_SAMPLES}u; i++) {
                let h = importance_sample_ggx(hammersley(i, ${PREFILTER_SAMPLES}u), n, roughness);
                let l = normalize(2.0 * dot(n, h) * h - n);
                let n_dot_l = dot(n, l);
                if (n_dot_l > 0.0) {
                    let n_dot_h = max(dot(n, h), 0.0);
                    let pdf = distribution_ggx(n_dot_h, roughness) * 0.25 + 0.0001;
                    let sample_solid_angle = 1.0 / ($PREFILTER_SAMPLES.0 * pdf + 0.0001);
                    let lod = max(0.5 * log2(sample_solid_angle / texel_solid_angle) + 1.0, 0.0);
                    color += textureSampleLevel(source, source_sampler, l, lod).rgb * n_dot_l;
                    weight += n_dot_l;
                }
            }
            return vec4f(color / max(weight, 0.0001), 1.0);
        }
        """
            .trimIndent()
    }

    private fun shaderBrdf() =
        """
        $COMMON
        $SAMPLING

        fn geometry_schlick_ggx(n_dot_v: f32, roughness: f32) -> f32 {
            let k = roughness * roughness / 2.0;
            return n_dot_v / (n_dot_v * (1.0 - k) + k);
        }

        @fragment
        fn fs_brdf(input: VertexOutput) -> @location(0) vec4f {
            let n_dot_v = max(input.uv.x, 0.0001);
            let roughness = input.uv.y;
            let v = vec3f(sqrt(1.0 - n_dot_v * n_dot_v), 0.0, n_dot_v);
            let n = vec3f(0.0, 0.0, 1.0);
            var a = 0.0;
            var b = 0.0;
            for (var i = 0u; i < ${BRDF_SAMPLES}u; i++) {
                let h = importance_sample_ggx(hammersley(i, ${BRDF_SAMPLES}u), n, roughness);
                let l = normalize(2.0 * dot(v, h) * h - v);
                let n_dot_l = max(l.z, 0.0);
                if (n_dot_l > 0.0) {
                    let n_dot_h = max(h.z, 0.0);
                    let v_dot_h = max(dot(v, h), 0.0);
                    let g = geometry_schlick_ggx(n_dot_v, roughness) * geometry_schlick_ggx(n_dot_l, roughness);
                    let g_vis = g * v_dot_h / (n_dot_h * n_dot_v);
                    let fc = pow(1.0 - v_dot_h, 5.0);
                    a += (1.0 - fc) * g_vis;
                    b += fc * g_vis;
                }
            }
            return vec4f(a / $BRDF_SAMPLES.0, b / $BRDF_SAMPLES.0, 0.0, 1.0);
        }
        """
            .trimIndent()

    override fun release() {
        cubeView.release()
        irradianceView.release()
        prefilteredView.release()
        brdfView.release()
        cubeTexture.release()
        irradianceTexture.release()
        prefilteredTexture.release()
        brdfTexture.release()
        sampler.release()
        uniformBuffer.release()
    }

    companion object {
        private val FORMAT = TextureFormat.RGBA16_FLOAT
        private val BRDF_FORMAT = TextureFormat.RG16_FLOAT
        private const val BRDF_SIZE = 256
        private const val BRDF_SAMPLES = 512
        private const val PREFILTER_SAMPLES = 512
        private const val IRRADIANCE_DELTA = 0.025

        private val COMMON =
            """
            const PI = 3.14159265359;

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) uv: vec2f,
                @location(1) @interpolate(flat) index: u32,
            };

            @vertex
            fn vs_main(@builtin(vertex_index) vertex: u32, @builtin(instance_index) instance: u32) -> VertexOutput {
                let xy = vec2f(f32((vertex << 1u) & 2u), f32(vertex & 2u));
                var output: VertexOutput;
                output.position = vec4f(xy * 2.0 - 1.0, 0.0, 1.0);
                output.uv = vec2f(xy.x, 1.0 - xy.y);
                output.index = instance;
                return output;
            }

            fn face_direction(face: u32, uv: vec2f) -> vec3f {
                let s = uv.x * 2.0 - 1.0;
                let t = uv.y * 2.0 - 1.0;
                var dir: vec3f;
                switch (face) {
                    case 0u: { dir = vec3f(1.0, -t, -s); }
                    case 1u: { dir = vec3f(-1.0, -t, s); }
                    case 2u: { dir = vec3f(s, 1.0, t); }
                    case 3u: { dir = vec3f(s, -1.0, -t); }
                    case 4u: { dir = vec3f(s, -t, 1.0); }
                    default: { dir = vec3f(-s, -t, -1.0); }
                }
                return normalize(dir);
            }
            """
                .trimIndent()

        private val SAMPLING =
            """
            fn hammersley(i: u32, count: u32) -> vec2f {
                return vec2f(f32(i) / f32(count), f32(reverseBits(i)) * 2.3283064365386963e-10);
            }

            fn importance_sample_ggx(xi: vec2f, n: vec3f, roughness: f32) -> vec3f {
                let a = roughness * roughness;
                let phi = 2.0 * PI * xi.x;
                let cos_theta = sqrt((1.0 - xi.y) / (1.0 + (a * a - 1.0) * xi.y));
                let sin_theta = sqrt(1.0 - cos_theta * cos_theta);
                let h = vec3f(cos(phi) * sin_theta, sin(phi) * sin_theta, cos_theta);
                var up = vec3f(0.0, 0.0, 1.0);
                if (abs(n.z) > 0.999) {
                    up = vec3f(1.0, 0.0, 0.0);
                }
                let tangent = normalize(cross(up, n));
                let bitangent = cross(n, tangent);
                return normalize(tangent * h.x + bitangent * h.y + n * h.z);
            }

            fn distribution_ggx(n_dot_h: f32, roughness: f32) -> f32 {
                let a = roughness * roughness;
                let a2 = a * a;
                let d = n_dot_h * n_dot_h * (a2 - 1.0) + 1.0;
                return a2 / (PI * d * d);
            }
            """
                .trimIndent()
    }
}
