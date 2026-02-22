plugins {
    id("vigipro.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vigipro.feature.auth"
}

dependencies {
    implementation(project(":core:core-network"))

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(libs.turbine)
}
