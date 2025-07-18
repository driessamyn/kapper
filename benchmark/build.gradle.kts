plugins {
    id("kapper.library-conventions")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    implementation(project(":kapper-coroutines"))

    implementation(libs.bundles.jmh)
    implementation(libs.slf4j.simple)

    // other libraries to benchmark against
    implementation("org.hibernate.orm:hibernate-core:7.0.6.Final")
    implementation("org.hibernate.orm:hibernate-community-dialects:7.0.3.Final")
    implementation("org.ktorm:ktorm-core:4.1.1")
    implementation("org.ktorm:ktorm-support-postgresql:4.1.1")
    implementation("org.ktorm:ktorm-support-sqlite:4.1.1")

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
