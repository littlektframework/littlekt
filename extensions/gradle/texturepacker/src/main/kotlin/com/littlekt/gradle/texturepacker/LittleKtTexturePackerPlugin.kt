package com.littlekt.gradle.texturepacker

import com.littlekt.tools.texturepacker.TexturePacker
import com.littlekt.tools.texturepacker.TexturePackerConfig
import com.littlekt.util.packer.PackingOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * @author Colton Daily
 * @date 1/27/2022
 */
@Suppress("unused")
class LittleKtTexturePackerPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.tasks.register("packTextures", Task::class.java) {
            it.group = "texture packer"
            it.doLast {
                val packer = TexturePacker(project.littleKt.texturePackerConfig)
                packer.process()
                packer.pack()
            }
        }
    }
}

fun Project.littleKt(action: LittleKtConfig.() -> Unit) = littleKt.apply(action)

val Project.littleKt: LittleKtConfig
    get() {
        val block = project.extensions.findByName("littlekt") as? LittleKtConfig?
        return if (block == null) {
            val newBlock = LittleKtConfig()
            project.extensions.add("littlekt", newBlock)
            newBlock
        } else {
            block
        }
    }

fun LittleKtConfig.texturePacker(action: TexturePackerConfig.() -> Unit) =
    texturePackerConfig.apply(action)

fun TexturePackerConfig.packing(action: PackingOptions.() -> Unit) = packingOptions.apply(action)
