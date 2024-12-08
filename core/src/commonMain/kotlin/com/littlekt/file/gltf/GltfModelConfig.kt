package com.littlekt.file.gltf

import com.littlekt.graphics.g3d.PBRMaterial
import com.littlekt.graphics.g3d.material.UnlitMaterial

/**
 * A configuration class that is used when generating a GLtf model.
 *
 * @param pbr if `true`, then generate a Model with a [PBRMaterial]; otherwise will use an
 *   [UnlitMaterial]
 * @param castShadows if `true`, then designate the model to cast shadows.
 * @author Colton Daily
 * @date 12/8/2024
 */
data class GltfModelConfig(val pbr: Boolean, val castShadows: Boolean)

/** Creates a [GltfModelConfig], specific for PBR usage. */
fun GltfModelPbrConfig(castShadows: Boolean = true) =
    GltfModelConfig(pbr = true, castShadows = castShadows)
