plugins {
    // Snippets are illustrative code that is compiled, never published — the same shape as the
    // examples. Being compiled by `./gradlew check` is what stops the docs from rotting.
    id("kapper.example-conventions")
}

dependencies {
    implementation(project(":kapper"))
    implementation(project(":kapper-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hikari)
}
