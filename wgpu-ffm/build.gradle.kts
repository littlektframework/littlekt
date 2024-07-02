plugins {
    id("java-library")
    id("module.publication")
}

tasks.withType<JavaExec> { jvmArgs("--enable-preview") }

tasks.withType<JavaCompile> { options.compilerArgs.add("--enable-preview") }

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

publishing { publications { create<MavenPublication>("wgpu-ffm") { from(components["java"]) } } }
