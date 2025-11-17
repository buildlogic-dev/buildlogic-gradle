import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.util.SystemReader
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.lib.Config
import java.util.Properties
import java.io.File

// Import statements for exception handling
import org.eclipse.jgit.api.errors.GitAPIException
import java.io.IOException

plugins{
    alias(libs.plugins.spotless) apply false
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(libs.jgit)
    }
}

// Extract Git information for build file/versioning use
val gitInfo = run {
    try {
        // Install a minimal SystemReader so JGit won't shell out to native git
        // for system/user config. This is a conservative implementation that
        // returns empty properties for system/user config and avoids running
        // external commands.
        SystemReader.setInstance(object : SystemReader() {
            override fun getHostname(): String = "localhost"

            override fun getenv(variable: String?): String? = System.getenv(variable)

            override fun getProperty(variable: String?): String? = System.getProperty(variable)

            // JGit 6+ expects these methods to return FileBasedConfig, taking the
            // requested Config and FS. We return empty FileBasedConfig instances so
            // JGit won't attempt to read system/user config or spawn external
            // processes. These are conservative no-op implementations.
            override fun openSystemConfig(base: Config?, fs: FS?): FileBasedConfig {
                val cfgDir = File(rootDir, "build/tmp/jgit-config")
                cfgDir.mkdirs()
                val f = File(cfgDir, "system.config")
                return object : FileBasedConfig(f, fs ?: FS.DETECTED) {
                    override fun load() {}
                    override fun save() {}
                }
            }

            override fun openUserConfig(base: Config?, fs: FS?): FileBasedConfig {
                val cfgDir = File(rootDir, "build/tmp/jgit-config")
                cfgDir.mkdirs()
                val f = File(cfgDir, "user.config")
                return object : FileBasedConfig(f, fs ?: FS.DETECTED) {
                    override fun load() {}
                    override fun save() {}
                }
            }

            override fun openJGitConfig(base: Config?, fs: FS?): FileBasedConfig {
                val cfgDir = File(rootDir, "build/tmp/jgit-config")
                cfgDir.mkdirs()
                val f = File(cfgDir, "jgit.config")
                return object : FileBasedConfig(f, fs ?: FS.DETECTED) {
                    override fun load() {}
                    override fun save() {}
                }
            }

            override fun getCurrentTime(): Long = System.currentTimeMillis()

            @Deprecated("override of deprecated method kept for compatibility")
            override fun getTimezone(offset: Long): Int = 0
        })

        val repo = RepositoryBuilder()
            .readEnvironment()
            .findGitDir(rootDir)
            .build()
        val git = Git(repo)
        val head = repo.resolve("HEAD")
        val commit = git.log().add(head).setMaxCount(1).call().firstOrNull()

        val branchName = repo.branch ?: "HEAD"
        val safeBranchName = branchName.replace(Regex("[^a-zA-Z0-9._-]"), "-")
        val commitHash = commit?.name ?: "unknown"
        val commitDateUtc = commit?.authorIdent?.getWhenAsInstant()?.atOffset(ZoneOffset.UTC)
        val commitDateUtcString = commit?.authorIdent?.getWhenAsInstant()
            ?.atOffset(ZoneOffset.UTC)
            ?.format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss")) ?: "unknown"

        mapOf(
            "GIT_BRANCH" to safeBranchName,
            "GIT_COMMIT" to commitHash,
            "GIT_COMMIT_DATE" to commitDateUtcString,
        )
    } catch (e: IOException) {
        println("IOException occurred: ${e.message}")
        emptyMap()
    } catch (e: GitAPIException) {
        println("GitAPIException occurred: ${e.message}")
        emptyMap()
    }
}

gitInfo.forEach { (k, v) ->
    println("$k=$v")
    extra[k] = v
}
version = "${gitInfo["GIT_BRANCH"]}-${gitInfo["GIT_COMMIT_DATE"]}-${gitInfo["GIT_COMMIT"]}"

subprojects {
    // Apply the version from the root project
    version = rootProject.version

    // ensure subprojects inherit group (redundant but explicit)
    group = "dev.buildlogic.gradle"

     if (file("build.gradle.kts").exists()) {
        if (!plugins.hasPlugin("com.diffplug.spotless")) {
            apply(plugin ="com.diffplug.spotless")
        }

        repositories {
            mavenCentral()
        }

        // Set up spotless for all Gradle build files
        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            kotlinGradle {
                target("*.gradle.kts") // default target for kotlinGradle
                ktlint() // or ktfmt() or prettier()
            }
        }
    }

    plugins.withId("java") {
        if (!plugins.hasPlugin("com.diffplug.spotless")) {
            apply(plugin ="com.diffplug.spotless")
        }

        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            java {
                targetExclude("build/generated/**")

                importOrder()
                removeUnusedImports()

                formatAnnotations()
                trimTrailingWhitespace()
                endWithNewline()
                leadingTabsToSpaces()

                licenseHeaderFile(rootProject.file("gradle/license-header.txt"))
            }
        }
    }

    // Configure kotlin formatting if the kotlin plugin is applied
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "com.diffplug.spotless")

        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            kotlin {
                ktlint()
            }
        }
    }
}
