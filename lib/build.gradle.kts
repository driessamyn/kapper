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
    alias(libs.plugins.deployer)

    id("maven-publish")
    id("signing")
}

semver {
    changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.defaultChangeLog
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
    currentProject {
        sources {
            excludedSourceSets.addAll("test", "integrationTest")
        }
    }
    reports {
        verify {
            rule {
                bound {
                    aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    coverageUnits = CoverageUnit.INSTRUCTION
                    minValue = 90
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

private val info =
    object {
        val name = project.name
        val groupId = project.group.toString()
        val version = project.version.toString()
        val description = "Kapper - A lightweight ORM for Kotlin and the JVM"
        val ghUser = "driessamyn"
        val ghProject = "kapper"
        val url = "https://github.com/$ghUser/$ghProject"
        val gitUrl = "$url.git"
        val issuesUrl = "$url/issues"
        val licence = "Apache-2.0"
        val licenceUrl = "https://opensource.org/licenses/Apache-2.0"
        val author = "Dries Samyn"
    }

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = info.groupId
            artifactId = info.name
            version = info.version
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}

deployer {
    println("Publishing version: $version")
//    verbose = true
    release.version = info.version
    projectInfo {
        name.set(info.name)
        description.set(info.description)
        url.set(info.url)
        groupId.set(info.groupId)
        artifactId.set(info.name)
        scm {
            fromGithub(info.ghUser, info.ghProject)
        }
        license(apache2)
        developer(info.ghUser, "dries@samyn.net")
    }
    content {
        component {
            fromMavenPublication("maven", clone = false)
        }
    }

    localSpec {
        directory.set(rootProject.layout.buildDirectory.get().dir("inspect"))
    }
    centralPortalSpec {
        auth.user.set(secret("MAVEN_USERNAME"))
        auth.password.set(secret("MAVEN_PASSWORD"))

        signing.key.set(secret("GPG_SIGNING_KEY"))
        signing.password.set(secret("GPG_SIGNING_PASSPHRASE"))
    }
    githubSpec {
        owner.set("driessamyn")
        repository.set("kapper")

        auth.user.set(secret("GH_USER"))
        auth.token.set(secret("GH_TOKEN"))
    }
}
