import org.gradle.kotlin.dsl.test

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter("test")

    useJUnitPlatform()
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.check {
    dependsOn(tasks.ktlintCheck)
    dependsOn("integrationTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(tasks.ktlintFormat)
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

koverReport {
    defaults {
        xml {
            onCheck = true
        }
        html {
            onCheck = true
        }
    }
    filters {
        excludes {
            // Add any exclusions if needed
            // classes("com.example.excluded.*")
        }
    }
//    verify {
//        rule {
//            isEnabled = true
//            bound {
//                minValue = 80 // Set your desired coverage threshold
//                valueType = kotlinx.kover.api.VerificationValueType.COVERED_LINES_PERCENTAGE
//            }
//        }
//    }
}

tasks.register("koverFullReport") {
    dependsOn(tasks.test, tasks["integrationTest"], tasks.koverXmlReport, tasks.koverHtmlReport)
}

tasks.test {
    finalizedBy(tasks.koverXmlReport, tasks.koverHtmlReport)
}

tasks.check {
    dependsOn(tasks.koverVerify)
}

dependencies {
    implementation(libs.kotlin.reflect)

    testImplementation(libs.mockito)
    testImplementation(libs.bundles.test)

    integrationTestImplementation(libs.bundles.test)
    integrationTestImplementation(libs.bundles.test.containers)
    integrationTestImplementation(libs.bundles.test.dbs)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
