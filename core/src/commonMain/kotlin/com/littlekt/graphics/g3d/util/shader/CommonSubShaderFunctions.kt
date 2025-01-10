package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute

fun SubShaderBuilder.colorConversionFunctions(
    useApproximateSrgb: Boolean = true,
    gamma: Float = 2.2f,
) {
    parts +=
        """
        fn linear_to_sRGB(linear : vec3f) -> vec3f {
            ${
            if(useApproximateSrgb) {
                """
                        let INV_GAMMA = 1.0 / $gamma;
                        return pow(linear, vec3(INV_GAMMA));
                    """.trimIndent()
            } else {
                """
                    if (all(linear <= vec3(0.0031308))) {
                        return linear * 12.92;
                    }
                    return (pow(abs(linear), vec3(1.0/2.4)) * 1.055) - vec3(0.055);
                    """.trimIndent()
            }
        }
        }
    """
}

fun SubShaderBuilder.vertexInput(attributes: List<VertexAttribute>) {
    val inputs =
        attributes.map { attribute ->
            when (attribute.usage) {
                VertexAttrUsage.POSITION ->
                    "@location(${attribute.shaderLocation}) position: vec4f,"
                VertexAttrUsage.COLOR -> "@location(${attribute.shaderLocation}) color: vec4f,"
                VertexAttrUsage.NORMAL -> "@location(${attribute.shaderLocation}) normal: vec3f,"
                VertexAttrUsage.TANGENT -> "@location(${attribute.shaderLocation}) tangent: vec4f,"
                VertexAttrUsage.UV -> {
                    if (attribute.index == 0) "@location(${attribute.shaderLocation}) uv: vec2f,"
                    else "@location(${attribute.shaderLocation}) uv2: vec2f,"
                }

                VertexAttrUsage.JOINT -> "@location(${attribute.shaderLocation}) joints: vec4i,"
                VertexAttrUsage.WEIGHT -> "@location(${attribute.shaderLocation}) weight: vec4f,"
                else -> {
                    error(
                        "Unknown attribute usage type: ${attribute.usage}. If using custom attributes, then you must create your own input struct!"
                    )
                }
            }
        }

    parts +=
        """
            struct VertexInput {
                ${inputs.joinToString("\n")}
            };
        """
            .trimIndent()
}

fun SubShaderBuilder.vertexOutput(attributes: List<VertexAttribute>) {
    parts +=
        """
        struct VertexOutput {
            @builtin(position) position: vec4f,
            @location(0) world_pos: vec3f,
            @location(1) normal: vec3f,
            @location(2) uv: vec2f,
            @location(3) uv2: vec2f,
            @location(4) color: vec4f,
            @location(5) normal: vec3f,
            ${
                if (attributes.any { it.usage == VertexAttrUsage.TANGENT }) {
                    """
                        @location(6) tangent: vec3f,
                        @location(7) bitangent: vec3f,
                    """.trimIndent()
                } else ""
            }
        };
    """
            .trimIndent()
}

/** Adds a Camera struct that only requires a 4x4 float matrix for the combined viewProjection. */
fun SubShaderBuilder.camera(group: Int, binding: Int) {
    parts +=
        """
        struct Camera {
            view_proj: mat4x4f,
        };
        @group($group) @binding($binding)
        var<uniform> camera: Camera;
        """
            .trimIndent()
}

/**
 * Adds a Camera struct that requires separated camera data such as the projection, inverse
 * projection, view, position, time, output size, near, and far. Expected to be used with PBR &
 * clustered shading.
 */
fun SubShaderBuilder.cameraWithLights(group: Int, binding: Int) {
    parts +=
        """
        struct Camera {
            proj: mat4x4f,
            inv_proj: mat4x4f,
            view: mat4x4f,
            position: vec3f,
            time: f32,
            output_size: vec2f,
            z_near: f32,
            z_far: f32,
        };
        @group($group) @binding($binding)
        var<uniform> camera: Camera;
        """
            .trimIndent()
}

fun SubShaderBuilder.model(group: Int, binding: Int) {
    parts +=
        """
        struct Model {
            transform: mat4x4f,
        };
        @group($group) @binding($binding)
        var<uniform> model: Model;
        """
}

fun SubShaderBuilder.light(group: Int, binding: Int) {
    parts +=
        """
        struct Light {
          position : vec3f,
          range : f32,
          color : vec3f,
          intensity : f32,
        };
    
        struct GlobalLights {
          ambient : vec3f,
          dir_color : vec3f,
          dir_intensity : f32,
          dir_direction : vec3f,
          light_count : u32,
          lights : array<Light>,
        };
        @group($group) @binding($binding)
        var<storage, read> global_lights : GlobalLights;
        """
}

fun SubShaderBuilder.skin(group: Int) {
    parts +=
        """
        struct Joints {
          matrices : array<mat4x4f>
        };
        @group(${group}) @binding(0) var<storage, read> joint : Joints;
        @group(${group}) @binding(1) var<storage, read> inverse_blend : Joints;
    """
            .trimIndent()
}

fun SubShaderBuilder.getSkinMatrix() {
    parts +=
        """
          fn get_skin_matrix(input : VertexInput) -> mat4x4f {
            let joint0 = joint.matrices[input.joints.x] * inverse_blend.matrices[input.joints.x];
            let joint1 = joint.matrices[input.joints.y] * inverse_blend.matrices[input.joints.y];
            let joint2 = joint.matrices[input.joints.z] * inverse_blend.matrices[input.joints.z];
            let joint3 = joint.matrices[input.joints.w] * inverse_blend.matrices[input.joints.w];

            let skin_matrix = joint0 * input.weights.x +
                             joint1 * input.weights.y +
                             joint2 * input.weights.z +
                             joint3 * input.weights.w;
            return skin_matrix;
          }
    """
            .trimIndent()
}
