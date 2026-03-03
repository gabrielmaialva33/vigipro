plugins {
    id("vigipro.android.feature")
}

android {
    namespace = "com.vigipro.feature.discover"
}

dependencies {
    implementation(project(":core:core-network"))
    implementation(libs.coil.compose)
    implementation(libs.timber)
}
