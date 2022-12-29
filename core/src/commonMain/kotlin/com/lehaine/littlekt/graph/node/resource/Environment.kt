package com.lehaine.littlekt.graph.node.resource

import com.lehaine.littlekt.graph.node.node3d.Light
import com.lehaine.littlekt.graph.node.render.ModelMaterial
import com.lehaine.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 12/27/2022
 */
class Environment {
    var ambientColor = Color.WHITE
    var ambientStrength = 0.1f

    var specularStrength = 0.5f

    var lights = mutableListOf<Light>()

    fun updateMaterial(material: ModelMaterial) {
        material.ambientStrength = ambientStrength
        material.specularStrength = specularStrength
        material.lightColor = ambientColor

        material.lightPosition = lights[0].globalCenter
    }
}