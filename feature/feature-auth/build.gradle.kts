plugins {
    id("vigipro.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vigipro.feature.auth"
}

dependencies {
    implementation(project(":core:core-network"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(libs.turbine)
}
