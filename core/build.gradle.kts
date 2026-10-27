dependencies {
    // Forge ships NightConfig; other loaders bundle this exact version.
    "implementation"(libs.nightconfig.toml)

    "testImplementation"(platform(libs.junit.bom))
    "testImplementation"(libs.junit.jupiter)
    "testImplementation"(libs.assertj)
    "testImplementation"(libs.jqwik)
    "testImplementation"(libs.archunit)
    "testRuntimeOnly"(libs.junit.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}
