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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

/**
 * Settings plugin that walks the repository and automatically includes every directory that
 * contains a Gradle build script while honoring {@code .nobuild} markers and user-configured
 * exclusions. The implementation defers execution until after the settings script is evaluated so
 * that callers can configure the {@link AutoIncludeSettingsExtension} before discovery begins.
 */
public final class AutoIncludeSettingsPlugin implements Plugin<Settings> {

    private static final String BUILD_GRADLE = "build.gradle";
    private static final String BUILD_GRADLE_KTS = "build.gradle.kts";
    private static final String NO_BUILD_MARKER = ".nobuild";

    /**
     * Registers the {@link AutoIncludeSettingsExtension} and schedules project discovery to run as
     * soon as Gradle finishes evaluating {@code settings.gradle(.kts)}. The deferred action
     * computes exclusion paths, filters candidate directories, converts them into Gradle project
     * paths, and invokes {@link Settings#include(String...)} in a deterministic order.
     *
     * @param settings the Gradle {@link Settings} instance associated with the current build
     */
    @Override
    public void apply(Settings settings) {
        Path rootPath = settings.getSettingsDir().toPath();
        Path buildSrcPath = rootPath.resolve("buildSrc");

        AutoIncludeSettingsExtension extension = new AutoIncludeSettingsExtension();
        // allow users to configure via settings: `autoInclude { excludedDirectories = listOf("foo") }`
        settings.getExtensions().add("autoInclude", extension);

        // Defer performing the file-walk until after the settings script has been fully
        // evaluated so that users can configure the `autoInclude` extension in their
        // `settings.gradle(.kts)` before we read its values.
        settings.getGradle().settingsEvaluated(evaluated -> {
            // resolve configured excluded directories to absolute paths
            final java.util.List<Path> configuredExcludes = new java.util.ArrayList<>();
            for (String d : extension.getExcludedDirectories()) {
                configuredExcludes.add(rootPath.resolve(d));
            }

            try (Stream<Path> paths = Files.walk(rootPath)) {
                paths
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(rootPath))
                    .filter(path -> !startsWith(path, buildSrcPath))
                    .filter(path -> configuredExcludes.stream().noneMatch(ex -> path.startsWith(ex)))
                    .filter(path -> hasBuildScript(path) && !hasNoBuildMarker(path))
                    .map(path -> toGradlePath(rootPath, path))
                    .sorted(Comparator.naturalOrder())
                    .forEach(settings::include);
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to auto-include Gradle projects", ex);
            }
        });
    }

    /**
     * Determines whether the supplied directory contains either a {@code build.gradle} or
     * {@code build.gradle.kts} file.
     *
     * @param directory candidate directory
     * @return {@code true} if a build script is present, otherwise {@code false}
     */
    private static boolean hasBuildScript(Path directory) {
        return Files.isRegularFile(directory.resolve(BUILD_GRADLE))
            || Files.isRegularFile(directory.resolve(BUILD_GRADLE_KTS));
    }

    /**
     * Checks whether the directory contains the opt-out marker {@code .nobuild}.
     *
     * @param directory candidate directory
     * @return {@code true} if the marker is present, else {@code false}
     */
    private static boolean hasNoBuildMarker(Path directory) {
        return Files.isRegularFile(directory.resolve(NO_BUILD_MARKER));
    }

    /**
     * Tests whether a candidate directory resides under a particular prefix and guards against
     * missing directory trees (e.g., {@code buildSrc}).
     *
     * @param candidate directory being evaluated
     * @param prefix directory that should be treated as an exclusion root
     * @return {@code true} when the candidate is within the prefix
     */
    private static boolean startsWith(Path candidate, Path prefix) {
        return Files.exists(prefix) && candidate.startsWith(prefix);
    }

    /**
     * Converts an absolute project directory into the Gradle path notation (e.g., {@code :lib}).
     *
     * @param rootPath root directory for the settings script
     * @param projectPath directory to convert into a Gradle project path
     * @return the Gradle path suitable for {@link Settings#include(String...)}
     */
    private static String toGradlePath(Path rootPath, Path projectPath) {
        String relative = rootPath.relativize(projectPath).toString().replace(File.separatorChar, ':');
        return ":" + relative;
    }
}
