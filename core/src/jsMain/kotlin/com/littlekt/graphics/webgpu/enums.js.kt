package com.littlekt.graphics.webgpu

val AlphaMode.nativeVal: String
    get() =
        when (this) {
            AlphaMode.OPAQUE -> "opaque"
            AlphaMode.PREMULTIPLIED -> "premultiplied"
            else -> error("AlphaMode.$name is unsupported on the web!")
        }

val TextureFormat.nativeVal: String
    get() =
        when (this) {
            /** 8 Bit Red channel only. `[0, 255]` converted to/from float `[0, 1]` in shader. */
            TextureFormat.R8_UNORM -> "r8unorm"

            /**
             * 8 Bit Red channel only. `[-127, 127]` converted to/from float `[-1, 1]` in shader.
             */
            TextureFormat.R8_SNORM -> "r8snorm"

            /** Red channel only. 8 bit integer per channel. Unsigned in shader. */
            TextureFormat.R8_UINT -> "r8uint"

            /** Red channel only. 8 bit integer per channel. Signed in shader. */
            TextureFormat.R8_SINT -> "r8sint"

            /** Red channel only. 16 bit integer per channel. Unsigned in shader. */
            TextureFormat.R16_UINT -> "r16uint"

            /** Red channel only. 16 bit integer per channel. Signed in shader. */
            TextureFormat.R16_SINT -> "r16sint"

            /** Red channel only. 16 bit float per channel. Float in shader. */
            TextureFormat.R16_FLOAT -> "r16float"

            /**
             * Red and green channels. 8 bit integer per channel. `[0, 255]` converted to/from float
             * `[0, 1]` in shader.
             */
            TextureFormat.RG8_UNORM -> "rg8unorm"

            /**
             * Red and green channels. 8 bit integer per channel. `[-127, 127]` converted to/from
             * float `[-1, 1]` in shader.
             */
            TextureFormat.RG8_SNORM -> "rg8snorm"

            /** Red and green channels. 8 bit integer per channel. Unsigned in shader. */
            TextureFormat.RG8_UINT -> "rg8uint"

            /** Red and green channel s. 8 bit integer per channel. Signed in shader. */
            TextureFormat.RG8_SINT -> "rg8sint"

            /** Red channel only. 32 bit integer per channel. Unsigned in shader. */
            TextureFormat.R32_UINT -> "r32uint"

            /** Red channel only. 32 bit integer per channel. Signed in shader. */
            TextureFormat.R32_SINT -> "r32sint"

            /** Red channel only. 32 bit float per channel. Float in shader. */
            TextureFormat.R32_FLOAT -> "r32float"

            /** Red and green channels. 16 bit integer per channel. Unsigned in shader. */
            TextureFormat.RG16_UINT -> "rg16uint"

            /** Red and green channels. 16 bit integer per channel. Signed in shader. */
            TextureFormat.RG16_SINT -> "rg16sint"

            /** Red and green channels. 16 bit float per channel. Float in shader. */
            TextureFormat.RG16_FLOAT -> "rg16float"

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. `[0, 255]` converted
             * to/from float `[0, 1]` in shader.
             */
            TextureFormat.RGBA8_UNORM -> "rgba8unorm"

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. Srgb-color `[0,
             * 255]` converted to/from linear-color float `[0, 1]` in shader.
             */
            TextureFormat.RGBA8_UNORM_SRGB -> "rgba8unorm-srgb"

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. `[-127, 127]`
             * converted to/from float `[-1, 1]` in shader.
             */
            TextureFormat.RGBA8_SNORM -> "rgba8snorm"

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. Unsigned in shader.
             */
            TextureFormat.RGBA8_UINT -> "rgba8uint"

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. Signed in shader.
             */
            TextureFormat.RGBA8_SINT -> "rgba8sint"

            /**
             * Blue, green, red, and alpha channels. 8 bit integer per channel. `[0, 255]` converted
             * to/from float `[0, 1]` in shader.
             */
            TextureFormat.BGRA8_UNORM -> "bgra8unorm"

            /**
             * Blue, green, red, and alpha channels. 8 bit integer per channel. Srgb-color `[0,
             * 255]` converted to/from linear-color float `[0, 1]` in shader.
             */
            TextureFormat.BGRA8_UNORM_SRGB -> "bgra8unorm-srgb"

            /**
             * Red, green, blue, and alpha channels. 10 bit integer for RGB channels, 2 bit integer
             * for alpha channel. `[0, 1023]` (`[0, 3]` for alpha) converted to/from float `[0, 1]`
             * in shader.
             */
            TextureFormat.RGB10A2_UNORM -> "rgb10a2unorm"

            /**
             * Red, green, and blue channels. 11 bit float with no sign bit for RG channels. 10 bit
             * float with no sign bit for blue channel. Float in shader.
             */
            TextureFormat.RG11B10_FLOAT -> "rg11b10ufloat"

            /** Red and green channels. 32 bit integer per channel. Unsigned in shader. */
            TextureFormat.RG32_UINT -> "rg32uint"

            /** Red and green channels. 32 bit integer per channel. Signed in shader. */
            TextureFormat.RG32_SINT -> "rg32sint"

            /** Red and green channels. 32 bit float per channel. Float in shader. */
            TextureFormat.RG32_FLOAT -> "rg32float"

            /**
             * Red, green, blue, and alpha channels. 16 bit integer per channel. Unsigned in shader.
             */
            TextureFormat.RGBA16_UINT -> "rgba16uint"

            /**
             * Red, green, blue, and alpha channels. 16 bit integer per channel. Signed in shader.
             */
            TextureFormat.RGBA16_SINT -> "rgba16sint"

            /** Red, green, blue, and alpha channels. 16 bit float per channel. Float in shader. */
            TextureFormat.RGBA16_FLOAT -> "rgba16float"

            /**
             * Red, green, blue, and alpha channels. 32 bit integer per channel. Unsigned in shader.
             */
            TextureFormat.RGBA32_UINT -> "rgba32uint"

            /**
             * Red, green, blue, and alpha channels. 32 bit integer per channel. Signed in shader.
             */
            TextureFormat.RGBA32_SINT -> "rgba32sint"

            /** Red, green, blue, and alpha channels. 32 bit float per channel. Float in shader. */
            TextureFormat.RGBA32_FLOAT -> "rgba32float"

            /** Special depth format with 32 bit floating point depth. */
            TextureFormat.DEPTH32_FLOAT -> "depth32float"

            /** Special depth format with at least 24 bit integer depth. */
            TextureFormat.DEPTH24_PLUS -> "depth24plus"

            /**
             * Special depth/stencil format with at least 24 bit integer depth and 8 bits integer
             * stencil.
             */
            TextureFormat.DEPTH24_PLUS_STENCIL8 -> "depth24plus-stencil8"
        }

fun TextureFormat.Companion.from(nativeVal: String) =
    TextureFormat.entries.firstOrNull { it.nativeVal == nativeVal }

val IndexFormat.nativeVal: String
    get() =
        when (this) {
            IndexFormat.UINT16 -> "uint16"
            IndexFormat.UINT32 -> "uint32"
        }

val TextureDimension.nativeVal: String
    get() =
        when (this) {
            TextureDimension.D1 -> "1d"
            TextureDimension.D2 -> "2d"
            TextureDimension.D3 -> "3d"
        }

val TextureViewDimension.nativeVal: String
    get() =
        when (this) {
            TextureViewDimension.D1 -> "1d"
            TextureViewDimension.D2 -> "2d"
            TextureViewDimension.D2_ARRAY -> "2d-array"
            TextureViewDimension.CUBE -> "cube"
            TextureViewDimension.CUBE_ARRAY -> "cube-array"
            TextureViewDimension.D3 -> "3d"
        }

val TextureAspect.nativeVal: String
    get() =
        when (this) {
            TextureAspect.ALL -> "all"
            TextureAspect.STENCIL_ONLY -> "stencil-only"
            TextureAspect.DEPTH_ONLY -> "depth-only"
        }

val VertexStepMode.nativeVal: String
    get() =
        when (this) {
            VertexStepMode.VERTEX -> "vertex"
            VertexStepMode.INSTANCE -> "instance"
        }

val VertexFormat.nativeVal: String
    get() =
        when (this) {
            VertexFormat.UINT8x2 -> "uint8x2"
            VertexFormat.UINT8x4 -> "uint8x4"
            VertexFormat.SINT8x2 -> "sint8x2"
            VertexFormat.SINT8x4 -> "sint8x4"
            VertexFormat.UNORM8x2 -> "unorm8x2"
            VertexFormat.UNORM8x4 -> "unorm8x4"
            VertexFormat.SNORM8x2 -> "snorm8x2"
            VertexFormat.SNORM8x4 -> "snorm8x4"
            VertexFormat.UINT16x2 -> "uint16x2"
            VertexFormat.UINT16x4 -> "uint16x4"
            VertexFormat.SINT16x2 -> "sint16x2"
            VertexFormat.SINT16x4 -> "sint16x4"
            VertexFormat.UNORM16x2 -> "unorm16x2"
            VertexFormat.UNORM16x4 -> "unorm16x4"
            VertexFormat.SNORM16x2 -> "snorm16x2"
            VertexFormat.SNORM16x4 -> "snorm16x4"
            VertexFormat.FLOAT16x2 -> "float16x2"
            VertexFormat.FLOAT16x4 -> "float16x4"
            VertexFormat.FLOAT32 -> "float32"
            VertexFormat.FLOAT32x2 -> "float32x2"
            VertexFormat.FLOAT32x3 -> "float32x3"
            VertexFormat.FLOAT32x4 -> "float32x4"
            VertexFormat.UINT32 -> "uint32"
            VertexFormat.UINT32x2 -> "uint32x2"
            VertexFormat.UINT32x3 -> "uint32x3"
            VertexFormat.UINT32x4 -> "uint32x4"
            VertexFormat.SINT32 -> "sint32"
            VertexFormat.SINT32x2 -> "sint32x2"
            VertexFormat.SINT32x3 -> "sint32x3"
            VertexFormat.SINT32x4 -> "sint32x4"
        }

val PrimitiveTopology.nativeVal: String
    get() =
        when (this) {
            PrimitiveTopology.POINT_LIST -> "point-list"
            PrimitiveTopology.LINE_LIST -> "line-list"
            PrimitiveTopology.LINE_STRIP -> "line-strip"
            PrimitiveTopology.TRIANGLE_LIST -> "triangle-list"
            PrimitiveTopology.TRIANGLE_STRIP -> "triangle-strip"
        }

val FrontFace.nativeVal: String
    get() =
        when (this) {
            FrontFace.CCW -> "ccw"
            FrontFace.CW -> "cw"
        }

val CullMode.nativeVal: String
    get() =
        when (this) {
            CullMode.NONE -> "none"
            CullMode.FRONT -> "front"
            CullMode.BACK -> "back"
        }

val CompareFunction.nativeVal: String
    get() =
        when (this) {
            CompareFunction.NEVER -> "never"
            CompareFunction.LESS -> "less"
            CompareFunction.EQUAL -> "equal"
            CompareFunction.LESS_EQUAL -> "less-equal"
            CompareFunction.GREATER -> "greater"
            CompareFunction.NOT_EQUAL -> "not-equal"
            CompareFunction.GREATER_EQUAL -> "greater-equal"
            CompareFunction.ALWAYS -> "always"
        }

val StencilOperation.nativeVal: String
    get() =
        when (this) {
            StencilOperation.KEEP -> "keep"
            StencilOperation.ZERO -> "zero"
            StencilOperation.REPLACE -> "replace"
            StencilOperation.INVERT -> "invert"
            StencilOperation.INCREMENT_CLAMP -> "increment-clamp"
            StencilOperation.DECREMENT_CLAMP -> "decrement-clamp"
            StencilOperation.INCREMENT_WRAP -> "increment-wrap"
            StencilOperation.DECREMENT_WRAP -> "decrement-wrap"
        }

val BlendOperation.nativeVal: String
    get() =
        when (this) {
            BlendOperation.ADD -> "add"
            BlendOperation.SUBTRACT -> "subtract"
            BlendOperation.REVERSE_SUBTRACT -> "reverse-subtract"
            BlendOperation.MIN -> "min"
            BlendOperation.MAX -> "max"
        }

val BlendFactor.nativeVal: String
    get() =
        when (this) {
            BlendFactor.ZERO -> "zero"
            BlendFactor.ONE -> "one"
            BlendFactor.SRC_COLOR -> "src"
            BlendFactor.ONE_MINUS_SRC_COLOR -> "one-minus-src"
            BlendFactor.SRC_ALPHA -> "src-alpha"
            BlendFactor.ONE_MINUS_SRC_ALPHA -> "one-minus-src-alpha"
            BlendFactor.DST_COLOR -> "dst"
            BlendFactor.ONE_MINUS_DST_COLOR -> "one-minus-dst"
            BlendFactor.DST_ALPHA -> "dst-alpha"
            BlendFactor.ONE_MINUS_DST_ALPHA -> "one-minus-dst-alpha"
            BlendFactor.SRC_ALPHA_SATURATED -> "src-alpha-saturated"
            BlendFactor.CONSTANT_COLOR -> "constant"
            BlendFactor.ONE_MINUS_CONSTANT_COLOR -> "one-minus-constant"
        }

val BufferBindingType.nativeVal: String
    get() =
        when (this) {
            BufferBindingType.UNIFORM -> "uniform"
            BufferBindingType.STORAGE -> "storage"
            BufferBindingType.READ_ONLY_STORAGE -> "read-only-storage"
        }

val SamplerBindingType.nativeVal: String
    get() =
        when (this) {
            SamplerBindingType.FILTERING -> "filtering"
            SamplerBindingType.NON_FILTERING -> "non-filtering"
            SamplerBindingType.COMPARISON -> "comparison"
        }

val TextureSampleType.nativeVal: String
    get() =
        when (this) {
            TextureSampleType.FLOAT -> "float"
            TextureSampleType.DEPTH -> "depth"
            TextureSampleType.SINT -> "sint"
            TextureSampleType.UINT -> "uint"
        }

val AddressMode.nativeVal: String
    get() =
        when (this) {
            AddressMode.CLAMP_TO_EDGE -> "clamp-to-edge"
            AddressMode.REPEAT -> "repeat"
            AddressMode.MIRROR_REPEAT -> "mirror-repeat"
        }

val FilterMode.nativeVal: String
    get() =
        when (this) {
            FilterMode.NEAREST -> "nearest"
            FilterMode.LINEAR -> "linear"
        }

val LoadOp.nativeVal: String
    get() =
        when (this) {
            LoadOp.CLEAR -> "clear"
            LoadOp.LOAD -> "load"
        }

val StoreOp.nativeVal: String
    get() =
        when (this) {
            StoreOp.DISCARD -> "discard"
            StoreOp.STORE -> "store"
        }

val Feature.nativeVal: String
    get() =
        when (this) {
            Feature.DEPTH_CLIP_CONTROL -> "depth-clip-control"
            Feature.DEPTH32FLOAT_STENCIL18 -> "depth32float-stencil8"
            Feature.TEXTURE_COMPRESSION_BC -> "texture-compression-bc"
            Feature.TEXTURE_COMPRESSION_ETC2 -> "texture-compression-etc2"
            Feature.TEXTURE_COMPRESSION_ASTC -> "texture-compression-astc"
            Feature.TIMESTAMP_QUERY -> "timestamp-query"
            Feature.INDIRECT_FIRST_INSTANCE -> "indirect-first-instance"
            Feature.SHADER_F16 -> "shader-f16"
            Feature.RG11B10UFLOAT_RENDERABLE -> "rg11b10ufloat-renderable"
            Feature.BGRA8UNORM_STORAGE -> "bgra8unorm-storage"
            Feature.FLOAT32_FILTERABLE -> "float32-filterable"
            Feature.CLIP_DISTANCES -> "clip-distances"
            Feature.DUAL_SOURCE_BLENDING -> "dual-source-blending"
        }
