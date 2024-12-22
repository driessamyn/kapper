import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.kotlin.dsl.test
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.dokka)
    alias(libs.plugins.git.semver)

    id("maven-publish")
    id("signing")
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
    withSourcesJar()
    withJavadocJar()
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

    testLogging {
        showStandardStreams = true
    }

    // disable parallel running
    systemProperties(
        "junit.jupiter.execution.parallel.enabled" to "false",
    )
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()

    testLogging {
        showStandardStreams = true
    }

    // enable parallel running
    systemProperties(
        // Configuration parameters to execute classes in parallel but methods in same thread
        "junit.jupiter.execution.parallel.enabled" to "true",
        "junit.jupiter.execution.parallel.mode.default" to "concurrent",
        "junit.jupiter.execution.parallel.mode.classes.default" to "concurrent",
    )
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

kover {
    reports {
        verify {
            rule {
                bound {
                    aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    coverageUnits = CoverageUnit.INSTRUCTION
                    minValue = 95
                }
            }
        }
    }
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
    implementation(libs.slf4j)

    testImplementation(libs.mockito)
    testImplementation(libs.bundles.test)

    integrationTestImplementation(libs.bundles.test)
    integrationTestImplementation(libs.bundles.test.containers)
    integrationTestImplementation(libs.bundles.test.dbs)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                Visibility.PUBLIC,
                Visibility.PROTECTED,
            ),
        )

        perPackageOption {
            matchingRegex.set(".*internal.*")
            suppress.set(true)
        }
    }
}

group = "net.samyn"
version = semver.version

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            ),
        )
    }
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            pom {
                name.set(project.name)
                description.set("Kapper - A lightweight ORM for Kotlin and the JVM")
                url.set("https://github.com/driessamyn/kapper")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("driessamyn")
                        name.set("Dries Samyn")
                    }
                }
                scm {
                    url.set(
                        "https://github.com/driessamyn/kapper.git",
                    )
                    connection.set(
                        "scm:git:git://github.com/driessamyn/kapper.git",
                    )
                    developerConnection.set(
                        "scm:git:git://github.com/driessamyn/kapper.git",
                    )
                }
                issueManagement {
                    url.set("https://github.com/driessamyn/kapper/issues")
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                setUrl("https://maven.pkg.github.com/driessamyn/kapper")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = layout.buildDirectory.dir("repos/releases")
            val snapshotsRepoUrl = layout.buildDirectory.dir("repos/snapshots")
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
}

signing {
    useInMemoryPgpKeys(System.getenv("GPG_SIGNING_KEY"), System.getenv("GPG_SIGNING_PASSPHRASE"))
    sign(publishing.publications["maven"])
}
