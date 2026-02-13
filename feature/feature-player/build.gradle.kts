plugins {
    id("vigipro.android.feature")
}

android {
    namespace = "com.vigipro.feature.player"
}

dependencies {
    implementation(project(":core:core-network"))

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)
    implementation(libs.media3.common)

    // ONVIF for PTZ
    implementation(libs.onvif.camera)
}
