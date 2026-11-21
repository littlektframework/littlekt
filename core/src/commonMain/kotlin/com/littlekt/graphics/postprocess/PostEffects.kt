package com.littlekt.graphics.postprocess

import com.littlekt.graphics.webgpu.Device

/**
 * Smooths jagged edges with FXAA. Run it after colors are in a `[0, 1]` range, such as after a
 * [com.littlekt.graphics.Tonemapper].
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class FxaaEffect(device: Device) :
    ShaderPostEffect(
        device,
        """
        fn fxaa_luma(color: vec3f) -> f32 {
            return dot(sqrt(max(color, vec3f(0.0))), vec3f(0.299, 0.587, 0.114));
        }

        fn fxaa_sample(uv: vec2f) -> vec3f {
            return textureSampleLevel(source, source_sampler, uv, 0.0).rgb;
        }

        fn effect(uv: vec2f) -> vec4f {
            let t = post.texel;
            let center = textureSampleLevel(source, source_sampler, uv, 0.0);
            let nw = fxaa_luma(fxaa_sample(uv + vec2f(-1.0, -1.0) * t));
            let ne = fxaa_luma(fxaa_sample(uv + vec2f(1.0, -1.0) * t));
            let sw = fxaa_luma(fxaa_sample(uv + vec2f(-1.0, 1.0) * t));
            let se = fxaa_luma(fxaa_sample(uv + vec2f(1.0, 1.0) * t));
            let m = fxaa_luma(center.rgb);
            let luma_min = min(m, min(min(nw, ne), min(sw, se)));
            let luma_max = max(m, max(max(nw, ne), max(sw, se)));
            if (luma_max - luma_min < max(0.0312, luma_max * 0.125)) {
                return center;
            }
            var dir = vec2f(-((nw + ne) - (sw + se)), (nw + sw) - (ne + se));
            let reduce = max((nw + ne + sw + se) * 0.03125, 1.0 / 128.0);
            let rcp_min = 1.0 / (min(abs(dir.x), abs(dir.y)) + reduce);
            dir = clamp(dir * rcp_min, vec2f(-8.0), vec2f(8.0)) * t;
            let a = 0.5 * (fxaa_sample(uv + dir * (1.0 / 3.0 - 0.5)) + fxaa_sample(uv + dir * (2.0 / 3.0 - 0.5)));
            let b = a * 0.5 + 0.25 * (fxaa_sample(uv - dir * 0.5) + fxaa_sample(uv + dir * 0.5));
            let luma_b = fxaa_luma(b);
            if (luma_b < luma_min || luma_b > luma_max) {
                return vec4f(a, center.a);
            }
            return vec4f(b, center.a);
        }
        """,
        label = "fxaa effect",
    )

/**
 * Darkens the edges of the image toward its corners.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class VignetteEffect(device: Device) :
    ShaderPostEffect(
        device,
        """
        fn effect(uv: vec2f) -> vec4f {
            let color = textureSample(source, source_sampler, uv);
            let d = length(uv - 0.5) * 1.41421356;
            let radius = post.params[0].y;
            let v = smoothstep(radius, radius - post.params[0].z, d);
            return vec4f(color.rgb * mix(1.0 - post.params[0].x, 1.0, v), color.a);
        }
        """,
        paramCount = 3,
        label = "vignette effect",
    ) {
    /** How dark the corners get, from `0` to `1`. */
    var intensity: Float
        get() = getParam(0)
        set(value) = setParam(0, value)

    /** Where the darkening is full, from the center `0` to the corners `1`. */
    var radius: Float
        get() = getParam(1)
        set(value) = setParam(1, value)

    /** How far inside [radius] the darkening starts. */
    var softness: Float
        get() = getParam(2)
        set(value) = setParam(2, value)

    init {
        intensity = 0.4f
        radius = 1f
        softness = 0.6f
    }
}

/**
 * Adjusts the brightness, contrast, and saturation of the image.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class ColorAdjustEffect(device: Device) :
    ShaderPostEffect(
        device,
        """
        fn effect(uv: vec2f) -> vec4f {
            let color = textureSample(source, source_sampler, uv);
            var rgb = color.rgb * post.params[0].x;
            rgb = max((rgb - 0.18) * post.params[0].y + 0.18, vec3f(0.0));
            let luma = dot(rgb, vec3f(0.2126, 0.7152, 0.0722));
            rgb = max(mix(vec3f(luma), rgb, post.params[0].z), vec3f(0.0));
            return vec4f(rgb, color.a);
        }
        """,
        paramCount = 3,
        label = "color adjust effect",
    ) {
    /** Multiplies the colors. `1` keeps them as they are. */
    var brightness: Float
        get() = getParam(0)
        set(value) = setParam(0, value)

    /** Pushes colors away from middle gray. `1` keeps them as they are. */
    var contrast: Float
        get() = getParam(1)
        set(value) = setParam(1, value)

    /** `0` is grayscale and `1` keeps colors as they are. */
    var saturation: Float
        get() = getParam(2)
        set(value) = setParam(2, value)

    init {
        brightness = 1f
        contrast = 1f
        saturation = 1f
    }
}

/**
 * Splits the red and blue channels apart toward the edges of the image, like a cheap lens.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class ChromaticAberrationEffect(device: Device) :
    ShaderPostEffect(
        device,
        """
        fn effect(uv: vec2f) -> vec4f {
            let offset = (uv - 0.5) * post.params[0].x;
            let color = textureSample(source, source_sampler, uv);
            let r = textureSample(source, source_sampler, uv + offset).r;
            let b = textureSample(source, source_sampler, uv - offset).b;
            return vec4f(r, color.g, b, color.a);
        }
        """,
        paramCount = 1,
        label = "chromatic aberration effect",
    ) {
    /** How far apart the channels are at the edges, as a fraction of the image size. */
    var strength: Float
        get() = getParam(0)
        set(value) = setParam(0, value)

    init {
        strength = 0.01f
    }
}
