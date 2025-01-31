plugins {
    id("io.deepmedia.tools.deployer")
    id("maven-publish")
    id("signing")
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
    }

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = info.groupId
            artifactId = info.name
            version = info.version
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["dokkaJavadocJar"])
        }
    }
}

deployer {
    verbose = true

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
