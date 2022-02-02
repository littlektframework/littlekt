plugins {
    kotlin("jvm")
    java
    `java-gradle-plugin`
    id("littlekt.convention.publication")
}
val littleKtVersion: String by project
group = "com.lehaine.littlekt.gradle"
version = littleKtVersion

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(project(":tools"))
    implementation(gradleApi())
    implementation(localGroovy())
    compileOnly(gradleKotlinDsl())
}

gradlePlugin {
    plugins {
        create("littlektTexturePacker") {
            id = "com.lehaine.littlekt.gradle.texturepacker"
            displayName = "LittleKt Texture Packer Plugin"
            description = "A Gradle plugin that adds packing textures into an atlas"
            implementationClass = "com.lehaine.littlekt.gradle.texturepacker.LittleKtTexturePackerPlugin"
        }
    }
}

tasks {
    val publishAllPublications = false

    val publishJvmPublicationToMavenLocal by creating(Task::class) {
        dependsOn(when {
            publishAllPublications -> "publishToMavenLocal"
            else -> "publishPluginMavenPublicationToMavenLocal"
        })
    }

    afterEvaluate {
        val publishTaskOrNull = project.tasks.findByName(when {
            publishAllPublications -> "publishAllPublicationsToMavenRepository"
            else -> "publishPluginMavenPublicationToMavenRepository"
        })

        if (publishTaskOrNull != null) {
            val publishJvmPublicationToMavenRepository by creating(Task::class) {
                dependsOn(publishTaskOrNull)
            }
        }
    }

    val jvmTest by creating(Task::class) {
        dependsOn("test")
    }
}