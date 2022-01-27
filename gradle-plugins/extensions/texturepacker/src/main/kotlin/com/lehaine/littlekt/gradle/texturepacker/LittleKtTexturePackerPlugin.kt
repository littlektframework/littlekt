package com.lehaine.littlekt.gradle.texturepacker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.ExtensionAware
import java.io.File

/**
 * @author Colton Daily
 * @date 1/27/2022
 */
@Suppress("unused")
class LittleKtTexturePackerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val littleKtBlock =
            project.extensions.findByName("littlekt") as? ExtensionAware?
        val texturePackerConfig =
            littleKtBlock?.extensions?.findByName("texturePacker") as? TexturePackerConfig?
                ?: TexturePackerConfig(project)

        project.logger.log(LogLevel.INFO, "HI!")
        project.tasks.register("packTextures", Task::class.java) {
            it.group = "texture packer"
            it.doLast {
                File("${texturePackerConfig.outputDir}${texturePackerConfig.outputName}").createNewFile()
            }
        }
    }
}