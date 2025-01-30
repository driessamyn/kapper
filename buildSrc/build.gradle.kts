plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // plugins
    implementation(libs.deployer.plugin)
    implementation(libs.dokka.plugin)
    implementation(libs.dokka.javadoc.plugin)
    implementation(libs.git.semver.plugin)
    implementation(libs.ktlint.plugin)
    implementation(libs.kotlin.jvm.plugin)
    implementation(libs.kover.plugin)

    // dependencies
    testImplementation(libs.mockito)
    testImplementation(libs.bundles.test)
}