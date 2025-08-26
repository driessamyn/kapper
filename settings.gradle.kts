plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "4.1.1"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}

rootProject.name = "kapper"
include("benchmark","core", "coroutines")
project(":benchmark").name = "kapper-benchmark"
project(":core").name = "kapper"
project(":coroutines").name = "kapper-coroutines"
