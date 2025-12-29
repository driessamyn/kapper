plugins {
    id("kapper.example-conventions")

    `java-test-fixtures`

    // needed for hibernate!
    id("org.jetbrains.kotlin.plugin.jpa") version "2.3.0"
}

dependencies {
    implementation(project(":kapper")) // local kapper core
    implementation(project(":kapper-coroutines")) // local kapper coroutines
    implementation(libs.kotlinx.coroutines.core)
    // alternatives
    //  hibernate
    implementation(libs.bundles.hibernate)
    //  ktorm
    implementation(libs.bundles.ktorm)
    // test
    testImplementation(libs.bundles.example.test)
    testImplementation(libs.hikari)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly(libs.bundles.example.dbs)
    testRuntimeOnly(libs.slf4j.simple)

    testFixturesImplementation(project(":kapper"))
    testFixturesImplementation(libs.bundles.example.test)
    testFixturesImplementation(libs.hikari)
}
