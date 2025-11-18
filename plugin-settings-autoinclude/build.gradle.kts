plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
    systemProperty("org.gradle.testkit.dir", "$buildDir/test-kit")
}

gradlePlugin {
    plugins {
        register("autoIncludeSettings") {
            id = "dev.buildlogic.settings.autoinclude"
            implementationClass = "dev.buildlogic.gradle.settings.autoinclude.AutoIncludeSettingsPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = "dev.buildlogic.gradle"
            artifactId = "plugin-settings-autoinclude"
            version = project.version.toString()

            from(components["java"])
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
