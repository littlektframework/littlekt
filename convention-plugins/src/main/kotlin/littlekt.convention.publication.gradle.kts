import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing
import java.util.*

plugins {
    `maven-publish`
    signing
}

var secretKey: String? = null
var signingPassword:String? = null
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
    secretKey = System.getenv("SIGNING_SECRET_KEY")
    signingPassword = System.getenv("SIGNING_PASSWORD")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name]?.toString()

publishing {

    repositories {
        maven {
            name = "sonatype"
            setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
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

signing {
    setRequired({
        (project.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
    })
    useInMemoryPgpKeys(secretKey, signingPassword)
    sign(publishing.publications)
}