plugins {
    id("vigipro.android.feature")
}

android {
    namespace = "com.vigipro.feature.sites"
}

dependencies {
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(libs.turbine)
}
