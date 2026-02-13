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
}
