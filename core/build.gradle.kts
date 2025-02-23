plugins {
    id("kapper.library-conventions")
    id("kapper.library-publish")
}

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.slf4j)

    testImplementation(libs.mockito)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.slf4j.simple)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    integrationTestImplementation(libs.bundles.test)
    integrationTestImplementation(libs.bundles.test.containers)
    integrationTestImplementation(libs.bundles.test.dbs)
    integrationTestRuntimeOnly(libs.slf4j.simple)
    integrationTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
