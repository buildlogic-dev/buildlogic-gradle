plugins{
    alias(libs.plugins.spotless) apply false
}

subprojects {
	// ensure subprojects inherit group (redundant but explicit)
	group = "dev.buildlogic.gradle"

    plugins.withId("java") {
        apply(plugin ="com.diffplug.spotless")

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
}
