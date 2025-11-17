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

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative configuration exposed to {@code settings.gradle(.kts)} callers for the
 * {@link AutoIncludeSettingsPlugin}. At the moment it supports only a list of directories to skip
 * during auto-inclusion, but the type can grow as new toggles are required.
 */
public class AutoIncludeSettingsExtension {
    private List<String> excludedDirectories = new ArrayList<>();

    /**
     * Directories (relative to the settings root) that should be ignored even if they contain build
     * scripts.
     *
     * @return mutable list of directory names to exclude
     */
    public List<String> getExcludedDirectories() {
        return excludedDirectories;
    }

    /**
     * Replaces the current exclusion list. Callers may pass {@code null} or an empty list to clear
     * the exclusions, though {@code null} will be normalized to an empty list.
     *
     * @param excludedDirectories directories to skip during discovery
     */
    public void setExcludedDirectories(List<String> excludedDirectories) {
        this.excludedDirectories = excludedDirectories == null ? new ArrayList<>() : excludedDirectories;
    }
}
