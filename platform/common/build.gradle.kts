plugins {
    `java-library`
    id("net.neoforged.moddev.legacyforge")
}

val gametest = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
}

legacyForge {
    mcpVersion = property("minecraft_version") as String
    addModdingDependenciesTo(gametest)
}

dependencies {
    compileOnly(project(":core"))
    "gametestCompileOnly"(project(":core"))
}
