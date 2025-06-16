plugins {
    id("kapper.library-conventions")
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    implementation(project(":kapper-coroutines"))

    implementation(libs.bundles.jmh)
    implementation(libs.slf4j.simple)

    // other libraries to benchmark against
    implementation("org.hibernate.orm:hibernate-core:6.6.11.Final")
    implementation("org.hibernate.orm:hibernate-community-dialects:6.4.1.Final")
    implementation("org.ktorm:ktorm-core:4.1.1")
    implementation("org.ktorm:ktorm-support-postgresql:4.1.1")
    implementation("org.ktorm:ktorm-support-sqlite:4.1.1")

    // supported DBs
    implementation(libs.test.containers)
    implementation(libs.test.containers.postgresql)
    implementation(libs.sqlite.jdbc)

    // to support mapper tests (stubbing out ResultSet)
    implementation(libs.mockk)

    runtimeOnly(libs.postgresql.driver)

    testImplementation(libs.bundles.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    integrationTestImplementation(libs.bundles.test)
    integrationTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jmh {
    benchmarkMode.set(listOf("avgt"))
    failOnError.set(true)
    warmup.set("1s")
    warmupBatchSize.set(1)
    warmupIterations.set(1)
    iterations.set(1)
    timeOnIteration.set("1s")
    fork.set(1)
}
