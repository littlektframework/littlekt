import java.util.*
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `maven-publish`
    signing
}

ext["secretKey"] = null

ext["signingPassword"] = null

ext["ossrhUsername"] = null

ext["ossrhPassword"] = null

val secretPropsFile = project.rootProject.file("local.properties")

if (secretPropsFile.exists()) {
    secretPropsFile
        .reader()
        .use { Properties().apply { load(it) } }
        .onEach { (name, value) -> ext[name.toString()] = value }
} else {
    ext["secretKey"] = System.getenv("SIGNING_SECRET_KEY")
    ext["signingPassword"] = System.getenv("SIGNING_PASSWORD")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

fun getExtraString(name: String) = ext[name]?.toString()

val littleKtVersion: String by project

@Suppress("UnstableApiUsage")
val hash: String by lazy {
    providers
        .exec { commandLine("git", "rev-parse", "--verify", "--short=7", "HEAD") }
        .standardOutput
        .asText
        .get()
        .trim()
}

publishing {
    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl =
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl =
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url =
                if (project.extra["isReleaseVersion"] as Boolean) releasesRepoUrl
                else snapshotsRepoUrl
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }
    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(
            tasks.register("${name}JavadocJar", Jar::class) {
                archiveClassifier.set("javadoc")
                archiveAppendix.set(this@withType.name)
            }
        )

        // Provide artifacts information required by Maven Central
        pom {
            name.set("LittleKt WebGPU Game Framework")
            description.set("Kotlin Multiplatform WebGPU 2D Game Framework")
            url.set("https://littlekt.com")

            licenses {
                license {
                    name.set("Apache 2.0")
                    url.set("https://github.com/littlektframework/littlekt/blob/master/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("LeHaine")
                    name.set("Colt Daily")
                    url.set("https://lehaine.com")
                }
            }
            scm { url.set("https://github.com/littlektframework/littlekt") }
        }
    }
}

tasks.withType<PublishToMavenRepository> {
    if (!(project.extra["isReleaseVersion"] as Boolean)) {
        version = littleKtVersion.removeSuffix("-SNAPSHOT") + ".$hash-SNAPSHOT"
    }
}

// region Fix Gradle warning about signing tasks using publishing task outputs without explicit
// dependencies
// https://github.com/gradle/gradle/issues/26091
tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}
// endregion

signing {
    setRequired({
        (project.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
    })
    useInMemoryPgpKeys(getExtraString("secretKey"), getExtraString("signingPassword"))
    sign(publishing.publications)
}
