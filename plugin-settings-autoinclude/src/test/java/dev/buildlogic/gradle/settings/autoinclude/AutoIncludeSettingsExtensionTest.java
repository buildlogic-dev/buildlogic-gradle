package dev.buildlogic.gradle.settings.autoinclude;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoIncludeSettingsExtensionTest {

    @Test
    void handlesNullExcludedDirectories() {
        AutoIncludeSettingsExtension extension = new AutoIncludeSettingsExtension();
        extension.setExcludedDirectories(null);
        assertNotNull(extension.getExcludedDirectories());
        assertTrue(extension.getExcludedDirectories().isEmpty());
    }
}
