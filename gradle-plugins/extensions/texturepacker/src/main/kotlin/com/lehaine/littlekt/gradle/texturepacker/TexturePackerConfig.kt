package com.lehaine.littlekt.gradle.texturepacker

import org.gradle.api.Project

/**
 * @author Colton Daily
 * @date 1/27/2022
 */
class TexturePackerConfig(val project: Project) {
    var outputName: String = "textures.atlas.json"
    var outputDir:String = "src/commonMain/resources/"
}