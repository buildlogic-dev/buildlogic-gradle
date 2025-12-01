plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.engine)

    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
    systemProperty(
        "org.gradle.testkit.dir",
        layout.buildDirectory
            .dir("test-kit")
            .get()
            .asFile,
    )
}

gradlePlugin {
    plugins {
        register("autoIncludeSettings") {
            id = "dev.buildlogic.settings.gitinfo"
            implementationClass = "dev.buildlogic.gradle.settings.gitinfo.GitInfoSettingsPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            groupId = if (rootProject.extra["GIT_BRANCH"]!!.equals("main")) "dev.buildlogic.gradle" else "dev.buildlogic.gradle.prerelease"
            artifactId = "plugin-settings-gitinfo"
            version = project.version.toString()

            from(components["java"])

            pom {
                name = "Gradle Plugin Settings GitInfo"
                description = "A Gradle plugin that automatically extracts Git information into Gradle project properties."
                url = "https://github.com/buildlogic-dev/buildlogic-gradle"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "nathanclayton"
                        name = "Nathan Clayton"
                        email = "nathanclayton@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:github.com/buildlogic-dev/buildlogic-gradle.git"
                    developerConnection = "scm:git:ssh://github.com/buildlogic-dev/buildlogic-gradle.git"
                    url = "https://github.com/buildlogic-dev/buildlogic-gradle"
                }
            }
        }
    }
    repositories {
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/buildlogic-dev/buildlogic-gradle/")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
