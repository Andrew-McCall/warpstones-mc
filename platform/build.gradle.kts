import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension

plugins {
    id("fabric-loom") version "1.19.3" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.148" apply false
}

val core = project(":core")
val common = project(":platform:common")
val forge = project(":platform:forge")
val neoforge = project(":platform:neoforge")
val fabric = project(":platform:fabric")
val targetMinecraft = property("minecraft_version") as String
val metadataProperties = mapOf(
    "version" to version,
    "minecraft_version" to targetMinecraft,
    "fabric_loader_version" to property("fabric_loader_version"),
)

configure(listOf(forge, neoforge, fabric)) {
    plugins.withType<JavaPlugin> {
        extensions.configure<BasePluginExtension> {
            archivesName = "warpstones-${project.name}-$targetMinecraft"
        }

        dependencies {
            "compileOnly"(core)
            "compileOnly"(common)
        }

        // Core and common are compiled into each loader's jar rather than shipped as separate jars.
        tasks.named<JavaCompile>("compileJava") {
            source(core.file("src/main/java"), common.file("src/main/java"))
        }

        // Shared GameTest logic is compiled into each loader's test source set, never into the release jar.
        the<SourceSetContainer>().matching { it.name == "gametest" }.configureEach {
            tasks.named<JavaCompile>(compileJavaTaskName) {
                source(common.file("src/gametest/java"))
            }
            tasks.named<ProcessResources>(processResourcesTaskName) {
                from(common.file("src/gametest/resources"))
            }
        }

        tasks.named<ProcessResources>("processResources") {
            from(common.file("src/main/resources"))
            inputs.properties(metadataProperties)
            filesMatching(listOf("META-INF/mods.toml", "fabric.mod.json")) {
                expand(metadataProperties)
            }
        }

        val gameTestTask = if (project == fabric) "runGameTest" else "runGameTestServer"
        val gameTestLog = file(if (project == fabric) "build/run/gameTest/logs/latest.log" else "run/logs/latest.log")
        // A server that fails to load mods still exits 0, so the log is the only reliable verdict.
        tasks.matching { it.name == gameTestTask }.configureEach {
            doFirst { gameTestLog.delete() }
            doLast {
                check(gameTestLog.isFile && gameTestLog.readText().contains("required tests passed")) {
                    "GameTests did not pass on ${project.name}; see $gameTestLog"
                }
            }
        }
        tasks.named("check") {
            dependsOn(gameTestTask)
        }
    }
}

// NeoForge 47.1 runs Forge 47 mods unchanged; the neoforge project builds the Forge sources against it to prove that.
configure(listOf(forge, neoforge)) {
    apply(plugin = "java-library")
    apply(plugin = "net.neoforged.moddev.legacyforge")

    val sourceSets = the<SourceSetContainer>()
    val main = sourceSets["main"]
    val gametest = sourceSets.create("gametest") {
        compileClasspath += main.output
        runtimeClasspath += main.output
    }

    if (project == neoforge) {
        tasks.named<JavaCompile>("compileJava") { source(forge.file("src/main/java")) }
        tasks.named<ProcessResources>("processResources") { from(forge.file("src/main/resources")) }
        tasks.named<JavaCompile>("compileGametestJava") { source(forge.file("src/gametest/java")) }
        tasks.named("jar") { enabled = false }
    } else {
        tasks.named("jar") { finalizedBy("reobfJar") }
    }

    extensions.configure<LegacyForgeExtension> {
        enable {
            if (project == forge) {
                forgeVersion = "$targetMinecraft-${property("forge_version")}"
            } else {
                neoForgeVersion = property("neoforge_version") as String
            }
        }
        addModdingDependenciesTo(gametest)

        runs {
            register("client") { client() }
            register("server") { server() }
            register("gameTestServer") {
                type = "gameTestServer"
                sourceSet = gametest
                systemProperty("forge.enabledGameTestNamespaces", "warpstones")
            }
        }

        mods {
            register("warpstones") {
                sourceSet(main)
                sourceSet(gametest)
            }
        }
    }
}
