plugins {
    id("vigipro.android.feature")
}

android {
    namespace = "com.vigipro.feature.dashboard"
}

dependencies {
    implementation(project(":core:core-network"))
    implementation(project(":feature:feature-player"))

    // Media3 for thumbnail/preview
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.ui)

    // Image loading
    implementation(libs.coil.compose)
}
