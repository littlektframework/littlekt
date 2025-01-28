plugins {
    kotlin("jvm")
    java
    `java-gradle-plugin`
    id("module.publication")
}

val littleKtVersion: String by project

group = "com.littlekt.gradle"

version = littleKtVersion

java { withSourcesJar() }

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(project(":extensions:tools")) {
        exclude(group = "org.lwjgl", module = "lwjgl")
        exclude(group = "org.lwjgl", module = "lwjgl-glfw")
        exclude(group = "org.lwjgl", module = "lwjgl-opengl")
        exclude(group = "org.lwjgl", module = "lwjgl-openal")
    }
    implementation(project(":core")) {
        exclude(group = "org.lwjgl", module = "lwjgl")
        exclude(group = "org.lwjgl", module = "lwjgl-glfw")
        exclude(group = "org.lwjgl", module = "lwjgl-opengl")
        exclude(group = "org.lwjgl", module = "lwjgl-openal")
    }
    implementation(gradleApi())
    implementation(localGroovy())
    compileOnly(gradleKotlinDsl())
}

gradlePlugin {
    plugins {
        create("littlektTexturePacker") {
            id = "com.littlekt.gradle.texturepacker"
            displayName = "LittleKt Texture Packer Plugin"
            description = "A Gradle plugin that adds packing textures into an atlas"
            implementationClass = "com.littlekt.gradle.texturepacker.LittleKtTexturePackerPlugin"
        }
    }
}

tasks {
    val publishAllPublications = false

    val publishJvmPublicationToMavenLocal by
        creating(Task::class) {
            dependsOn(
                when {
                    publishAllPublications -> "publishToMavenLocal"
                    else -> "publishPluginMavenPublicationToMavenLocal"
                }
            )
        }

    afterEvaluate {
        val publishTaskOrNull =
            project.tasks.findByName(
                when {
                    publishAllPublications -> "publishAllPublicationsToMavenRepository"
                    else -> "publishPluginMavenPublicationToMavenRepository"
                }
            )

        if (publishTaskOrNull != null) {
            val publishJvmPublicationToMavenRepository by
                creating(Task::class) { dependsOn(publishTaskOrNull) }
        }
    }

    val jvmTest by creating(Task::class) { dependsOn("test") }
}
