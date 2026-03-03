plugins {
    id("vigipro.android.feature")
}

android {
    namespace = "com.vigipro.feature.player"
}

dependencies {
    implementation(project(":core:core-network"))

    // Ktor (for ONVIF PTZ SOAP client)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)
    implementation(libs.media3.common)

    // ONVIF for PTZ
    implementation(libs.onvif.camera)

    // VLC fallback player (RTSP streams with non-standard SDP)
    implementation(libs.libvlc.all)
}
