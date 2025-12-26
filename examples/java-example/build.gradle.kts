plugins {
    id("kapper.example-conventions")
}

dependencies {
    implementation(project(":kapper")) // local kapper core

    testImplementation(libs.bundles.example.test)
    testImplementation(testFixtures(project(":examples:kapper-kotlin-example")))

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly(libs.bundles.example.dbs)
    testRuntimeOnly(libs.slf4j.simple)
}
