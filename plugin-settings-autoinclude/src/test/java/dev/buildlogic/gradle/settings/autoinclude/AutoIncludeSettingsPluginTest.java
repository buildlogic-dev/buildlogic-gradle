/*******************************************************************************
Copyright 2025 Nathan Clayton <nathanclayton@gmail.com> and contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*******************************************************************************/
package dev.buildlogic.gradle.settings.autoinclude;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Gradle TestKit backed integration tests that exercise the {@link AutoIncludeSettingsPlugin}
 * against temporary Gradle builds. Each test constructs a lightweight project layout on disk,
 * applies the plugin via {@code pluginManagement} (referencing the included build), then asserts on
 * the output of the {@code projects} task.
 */
public class AutoIncludeSettingsPluginTest {

    private Path testProjectDir;

    /**
     * Creates a scratch directory that represents the root of the synthetic Gradle build used by
     * an individual test.
     */
    @BeforeEach
    public void setup() throws IOException {
        testProjectDir = Files.createTempDirectory("gradle-test-");
    }

    /**
     * Recursively removes the scratch Gradle project created in {@link #setup()}.
     */
    @AfterEach
    public void teardown() throws IOException {
        if (testProjectDir != null) {
            Files.walk(testProjectDir)
                .sorted((a, b) -> b.compareTo(a))
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    /**
     * Verifies that directories containing build scripts are automatically included when no extra
     * exclusions or markers are present.
     */
    @Test
    public void includesProjectsUnlessExcluded() throws IOException {
        // create two subprojects
        Path a = testProjectDir.resolve("a");
        Path b = testProjectDir.resolve("b");
        Files.createDirectories(a);
        Files.createDirectories(b);
        Files.writeString(a.resolve("build.gradle"), "");
        Files.writeString(b.resolve("build.gradle"), "");

        // create settings that uses the plugin included build (absolute path)
        String pluginIncludePath = new File(System.getProperty("user.dir")).getAbsolutePath().replace("\\", "/");
        String settings = "pluginManagement { includeBuild('" + pluginIncludePath + "') }\nplugins { id('dev.buildlogic.settings.autoinclude') }\n";
        Files.writeString(testProjectDir.resolve("settings.gradle"), settings);

        Files.writeString(testProjectDir.resolve("build.gradle"), "");

        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("projects", "--stacktrace")
            .withPluginClasspath()
            .build();

        String output = result.getOutput();
        assertTrue(output.contains("Project ':a'"));
        assertTrue(output.contains("Project ':b'"));
    }

    /**
     * Ensures that values provided through the {@code autoInclude.excludedDirectories} extension
     * prevent matching directories from being included even if they contain build scripts.
     */
    @Test
    public void respectsExcludedDirectories() throws IOException {
        Path a = testProjectDir.resolve("a");
        Path b = testProjectDir.resolve("b");
        Files.createDirectories(a);
        Files.createDirectories(b);
        Files.writeString(a.resolve("build.gradle"), "");
        Files.writeString(b.resolve("build.gradle"), "");

        // configure settings to exclude 'b' (use absolute includeBuild path)
        String pluginIncludePath = new File(System.getProperty("user.dir")).getAbsolutePath().replace("\\", "/");
        String settings = "pluginManagement { includeBuild('" + pluginIncludePath + "') }\nplugins { id('dev.buildlogic.settings.autoinclude') }\nautoInclude { excludedDirectories = ['b'] }\n";
        Files.writeString(testProjectDir.resolve("settings.gradle"), settings);
        Files.writeString(testProjectDir.resolve("build.gradle"), "");

        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("projects", "--stacktrace")
            .withPluginClasspath()
            .build();

        String output = result.getOutput();
        assertTrue(output.contains("Project ':a'"));
        assertTrue(!output.contains("Project ':b'"));
    }

    /**
     * Validates that the presence of a {@code .nobuild} marker causes the plugin to skip a
     * directory that otherwise qualifies for inclusion.
     */
    @Test
    public void respectsNoBuildMarker() throws IOException {
        Path a = testProjectDir.resolve("a");
        Path b = testProjectDir.resolve("b");
        Files.createDirectories(a);
        Files.createDirectories(b);
        Files.writeString(a.resolve("build.gradle"), "");
        Files.writeString(b.resolve("build.gradle"), "");
        Files.writeString(b.resolve(".nobuild"), "");

        String pluginIncludePath = new File(System.getProperty("user.dir")).getAbsolutePath().replace("\\", "/");
        String settings = "pluginManagement { includeBuild('" + pluginIncludePath + "') }\nplugins { id('dev.buildlogic.settings.autoinclude') }\n";
        Files.writeString(testProjectDir.resolve("settings.gradle"), settings);
        Files.writeString(testProjectDir.resolve("build.gradle"), "");

        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("projects", "--stacktrace")
            .withPluginClasspath()
            .build();

        String output = result.getOutput();
        assertTrue(output.contains("Project ':a'"));
        assertTrue(!output.contains("Project ':b'"));
    }

    /**
     * Confirms that directories located within generated subtrees such as {@code build/} are not
     * auto-included even if they contain build scripts.
     */
    @Test
    public void ignoresBuildSubdirectoriesWithBuildScripts() throws IOException {
        Path legit = testProjectDir.resolve("app");
        Path generated = testProjectDir.resolve("build/generated");
        Files.createDirectories(legit);
        Files.createDirectories(generated);
        Files.writeString(legit.resolve("build.gradle"), "");
        Files.writeString(generated.resolve("build.gradle"), "");

        String pluginIncludePath = new File(System.getProperty("user.dir")).getAbsolutePath().replace("\\", "/");
        String settings = "pluginManagement { includeBuild('" + pluginIncludePath + "') }\nplugins { id('dev.buildlogic.settings.autoinclude') }\n";
        Files.writeString(testProjectDir.resolve("settings.gradle"), settings);
        Files.writeString(testProjectDir.resolve("build.gradle"), "");

        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("projects", "--stacktrace")
            .withPluginClasspath()
            .build();

        String output = result.getOutput();
        assertTrue(output.contains("Project ':app'"));
        assertTrue(!output.contains("Project ':build:generated'"));
    }
}
