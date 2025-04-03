plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "me.f1nal"
repositories {
    mavenCentral()
    maven {
        setUrl("https://jitpack.io")
    }
    maven {
        setUrl("https://repository.ow2.org/nexus/content/repositories/snapshots")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":decompiler"))

    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("com.thoughtworks.xstream:xstream:1.4.21")
    implementation("org.tukaani:xz:1.9")

    implementation("org.ow2.asm:asm:9.8-SNAPSHOT")
    implementation("org.ow2.asm:asm-analysis:9.8-SNAPSHOT")
    implementation("org.ow2.asm:asm-commons:9.8-SNAPSHOT")
    implementation("org.ow2.asm:asm-tree:9.8-SNAPSHOT")
    implementation("org.ow2.asm:asm-util:9.8-SNAPSHOT")
    implementation("com.github.Col-E:CAFED00D:2.1.4")

    implementation("com.github.EpicPlayerA10.SSVM:mirrors:ca3c3ab713")
    implementation("com.github.EpicPlayerA10.SSVM:ssvm-core:ca3c3ab713")
    implementation("com.github.EpicPlayerA10.SSVM:ssvm-invoke:ca3c3ab713")
    implementation("com.github.EpicPlayerA10.SSVM:ssvm-io:ca3c3ab713")

    implementation("io.github.spair:imgui-java-app:1.86.11")
    implementation("com.github.steos:jnafilechooser:1.1.2")

    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("org.slf4j:slf4j-api:2.0.16")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.f1nal.trinity.Main"
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations = listOf(project.configurations.runtimeClasspath.get())
    manifest {
        attributes["Main-Class"] = "me.f1nal.trinity.Main"
    }
    mergeServiceFiles()
}

tasks.register<JavaExec>("run") {
    mainClass.set("me.f1nal.trinity.Main")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}