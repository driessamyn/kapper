plugins {
    id("kapper.library-conventions")
    id("kapper.library-publish")
}

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.slf4j)

    testImplementation(libs.mockito)
    testImplementation(libs.bundles.test)

    integrationTestImplementation(libs.bundles.test)
    integrationTestImplementation(libs.bundles.test.containers)
    integrationTestImplementation(libs.bundles.test.dbs)
}
