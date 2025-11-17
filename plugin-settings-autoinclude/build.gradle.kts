plugins {
    id("dev.buildlogic.java.library")
    id("maven-publish")
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
    repositories {
        maven {
            name = "GithubPackages"
            url = "https://maven.pkg.github.com/buildlogic-dev/gradle-plugin-settings-autoinclude/"
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
