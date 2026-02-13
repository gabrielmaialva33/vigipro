import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("vigipro.android.library")
                apply("vigipro.android.compose")
                apply("vigipro.android.hilt")
            }

            val libs = extensions.getByType(
                org.gradle.api.artifacts.VersionCatalogsExtension::class.java
            ).named("libs")

            dependencies {
                add("implementation", project(":core:core-ui"))
                add("implementation", project(":core:core-model"))
                add("implementation", project(":core:core-data"))

                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("navigation-compose").get())
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())

                add("implementation", libs.findLibrary("orbit-core").get())
                add("implementation", libs.findLibrary("orbit-compose").get())
                add("implementation", libs.findLibrary("orbit-viewmodel").get())
            }
        }
    }
}
