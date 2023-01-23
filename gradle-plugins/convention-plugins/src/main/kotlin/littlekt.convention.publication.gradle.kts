import java.util.*

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
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
} else {
    ext["secretKey"]  = System.getenv("SIGNING_SECRET_KEY")
    ext["signingPassword"] = System.getenv("SIGNING_PASSWORD")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name]?.toString()

val littleKtVersion: String by project
val hash: String by lazy {
    val stdout = java.io.ByteArrayOutputStream()
    rootProject.exec {
        commandLine("git", "rev-parse", "--verify", "--short=7", "HEAD")
        standardOutput = stdout
    }
    stdout.toString().trim()
}


publishing {
    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (project.extra["isReleaseVersion"] as Boolean) releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }

    // Configure all publications
    publications.withType<MavenPublication> {

        // Stub javadoc.jar artifact
        artifact(javadocJar.get())

        pom {
            name.set("LittleKt Game Framework")
            description.set("Kotlin Multiplatform 2D Game Framework")
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
                }
            }
            scm {
                url.set("https://github.com/littlektframework/littlekt")
            }

        }
    }
}

tasks.withType<PublishToMavenRepository> {
    if (!(project.extra["isReleaseVersion"] as Boolean)) {
        version = littleKtVersion.removeSuffix("-SNAPSHOT") + ".$hash-SNAPSHOT"
    }
}

signing {
    setRequired({
        (project.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
    })
    useInMemoryPgpKeys(getExtraString("secretKey"), getExtraString("signingPassword"))
    sign(publishing.publications)
}