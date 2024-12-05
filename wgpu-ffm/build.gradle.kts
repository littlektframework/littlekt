plugins {
    id("java-library")
    id("module.publication")
}

java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
    withSourcesJar()
}

publishing { publications { create<MavenPublication>("wgpu-ffm") { from(components["java"]) } } }
