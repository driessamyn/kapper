import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    `java-library`
    id("org.jetbrains.kotlinx.kover")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("dev.opensavvy.dokkatoo-mkdocs")
}

repositories {
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

kover {
    currentProject {
        sources {
            excludedSourceSets.addAll("test", "integrationTest", "jmh")
        }
        instrumentation {
            disabledForTestTasks.add("integrationTest")
        }
    }
    reports {
        filters {
            excludes {
                packages("net.samyn.kapper.benchmark")
            }
        }
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

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

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

    systemProperty("db", System.getProperty("db", ""))
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
        "junit.jupiter.execution.parallel.mode.default" to "same_thread",
        "junit.jupiter.execution.parallel.mode.classes.default" to "concurrent",
    )
}

tasks.check {
    dependsOn(tasks.ktlintCheck)
    dependsOn(tasks.koverVerify)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(tasks.ktlintFormat)
}

@Suppress("unused")
val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

tasks.test {
    finalizedBy(tasks.koverXmlReport, tasks.koverHtmlReport)
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

@Suppress("unused")
val dokkaJavadocJar by tasks.registering(Jar::class) {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

@Suppress("unused")
val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

// Generate markdown docs for the docs website using dokka-mkdocs
tasks.register("generateMarkdownDocs") {
    group = "documentation"
    description = "Generate markdown API documentation for the docs website"
    
    dependsOn(tasks.named("dokkatooGeneratePublicationMkdocs"))
    
    doLast {
        val projectDocsDir = rootProject.file("docs/api/${project.name}")
        projectDocsDir.deleteRecursively()
        projectDocsDir.mkdirs()
        
        val dokkaOutputDir = file("build/dokka/mkdocs")
        if (dokkaOutputDir.exists()) {
            // Copy from the nested module directory to flatten structure
            val moduleDir = dokkaOutputDir.listFiles()?.find { it.isDirectory }
            if (moduleDir != null) {
                copy {
                    from(moduleDir)
                    into(projectDocsDir)
                }
            }
        }
        
        // Remove internal packages
        projectDocsDir.walkTopDown()
            .filter { it.isDirectory && it.name.contains("internal") }
            .forEach { it.deleteRecursively() }
        
        // Fix .html links to .md and remove internal links in all markdown files
        projectDocsDir.walkTopDown()
            .filter { it.extension == "md" }
            .forEach { file ->
                val content = file.readText()
                val lines = content.lines()
                val filteredLines = lines.filter { line ->
                    // Remove lines that link to internal packages
                    !line.contains("net.samyn.kapper.internal")
                }
                val fixedContent = filteredLines.joinToString("\n")
                    .replace("/index.html)", ")")
                    .replace(".html)", ")")
                    .replace("](kapper/", "](./")
                    .replace("](${project.name}/", "](./")
                file.writeText(fixedContent)
            }
        
        println("Generated markdown API docs for ${project.name} in docs/api/${project.name}")
    }
}
