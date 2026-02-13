plugins {
    id("vigipro.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vigipro.feature.devices"
}

dependencies {
    implementation(project(":core:core-network"))

    // ONVIF discovery + management
    implementation(libs.onvif.camera)

    // QR Code scanning
    implementation(libs.barcode.scanning)
}
