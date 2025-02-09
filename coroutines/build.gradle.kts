plugins {
    id("kapper.library-conventions")
    id("kapper.library-publish")
}

dependencies {
    api(project(":kapper"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j)

    testImplementation(libs.mockito)
    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)

    integrationTestImplementation(libs.bundles.test)
    integrationTestImplementation(libs.bundles.test.containers)
    integrationTestImplementation(libs.bundles.test.dbs)
    integrationTestImplementation(libs.kotlinx.coroutines.test)
}
