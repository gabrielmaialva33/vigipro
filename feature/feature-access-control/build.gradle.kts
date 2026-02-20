plugins {
    id("vigipro.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vigipro.feature.accesscontrol"
}

dependencies {
    implementation(project(":core:core-network"))

    // QR Code generation
    implementation(libs.zxing.core)

    // QR Code scanning (ML Kit)
    implementation(libs.barcode.scanning)

    // CameraX (for QR scanner)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(libs.turbine)
}
