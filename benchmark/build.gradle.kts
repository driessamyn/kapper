plugins {
    id("kapper.library-conventions")
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(project(":kapper-coroutines"))

    implementation(libs.bundles.jmh)
    implementation(libs.slf4j.simple)

    // other libraries to benchmark against
    implementation(libs.hibernate.core)
    implementation(libs.hibernate.community.dialects)
    implementation(libs.ktorm.core)
    implementation(libs.ktorm.support.postgresql)
    implementation(libs.ktorm.support.sqlite)

    // supported DBs
    implementation(libs.test.containers)
    implementation(libs.test.containers.postgresql)
    implementation(libs.sqlite.jdbc)

    runtimeOnly(libs.postgresql.driver)

    testImplementation(libs.bundles.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    integrationTestImplementation(libs.bundles.test)
    integrationTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jmh {
    benchmarkMode.set(listOf("avgt"))
    failOnError.set(true)
    warmup.set("5s")
    warmupBatchSize.set(1)
    warmupIterations.set(1)
    fork.set(3)
}

tasks.register("jmhKapper") {
    group = "benchmarking"
    description = "Runs JMH benchmarks for KapperBenchmark classes."
    doFirst {
        jmh {
            includes.set(listOf(".*KapperBenchmark.*"))
            iterations.set(1)
            timeOnIteration.set("1s")
        }
    }
    finalizedBy("jmh")
}

tasks.register("jmhMapper") {
    group = "benchmarking"
    description = "Runs JMH benchmarks for MapperBenchmark classes."
    doFirst {
        jmh {
            includes.set(listOf(".*MapperBenchmark.*"))
            iterations.set(3)
            timeOnIteration.set("2s")
//            profilers.set(listOf("stack"))
        }
    }
    finalizedBy("jmh")
}

tasks.register<Zip>("benchmarkZip") {
    group = "benchmarking"
    description = "Zips the JMH benchmark JAR for distribution."
    dependsOn("jmhJar")

    archiveBaseName.set("kapper-benchmarks")
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    // Include only the specific JMH JAR that was just built
    from(tasks.named("jmhJar").get().outputs.files)

    // Include benchmark scripts and documentation
    from(projectDir) {
        include("run.sh")
        include("run-mapper.sh")
        include("run-usage.txt")
        include("README.md")
    }
}
