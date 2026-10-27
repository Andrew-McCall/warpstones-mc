plugins {
    id("fabric-loom")
}

val nightConfig = libs.versions.nightconfig.get()

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    implementation(libs.nightconfig.toml)
    include("com.electronwill.night-config:core:$nightConfig")
    include("com.electronwill.night-config:toml:$nightConfig")
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "warpstones-gametest"
        enableClientGameTests = false
        eula = true
    }
}

loom {
    runs {
        named("client") {
            runDir("run")
        }
        named("server") {
            runDir("run")
        }
    }
}
