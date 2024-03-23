import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

repositories {
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

kotlin {
    androidTarget()
    jvm {
        compilations {
            val main by getting

            val mainClass = "com.lehaine.littlekt.samples.DisplayTestKt"
            tasks {
                register<Copy>("copyResources") {
                    group = "package"
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    from(main.output.resourcesDir)
                    destinationDir = File("$buildDir/publish")
                }
                register<Jar>("packageFatJar") {
                    group = "package"
                    archiveClassifier.set("all")
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmJar"))
                    dependsOn(named("copyResources"))
                    manifest {
                        attributes["Main-Class"] = mainClass
                    }
                    destinationDirectory.set(File("$buildDir/publish/"))
                    from(
                        main.runtimeDependencyFiles.map { if (it.isDirectory) it else zipTree(it) },
                        main.output.classesDirs
                    )
                    doLast {
                        project.logger.lifecycle("[LittleKt] The packaged jar is available at: ${outputs.files.first().parent}")
                    }
                }

            }
        }
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(KotlinJsCompilerType.IR) {
        binaries.executable()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        this.attributes.attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.js
        )

        compilations.all {
            kotlinOptions.sourceMap = true
        }
    }

    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig(Action {
                devServer =
                    (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).copy(
                        open = mapOf(
                            "app" to mapOf(
                                "name" to "firefox"
                            )
                        ),
                    )
            })
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting

        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(libs.kotlinx.html.js)
            }

        }
        val jsTest by getting

        val wasmJsMain by getting
        val wasmJsTest by getting

        val androidMain by getting

        all {
            languageSettings.apply {
                progressiveMode = true
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }
    }
}

android {
    namespace = "com.lehaine.littlekt.samples"
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        assets.srcDirs("src/commonMain/resources")
    }
    compileSdk = (findProperty("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}