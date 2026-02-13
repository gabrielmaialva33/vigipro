plugins {
    id("vigipro.android.library")
    id("vigipro.android.compose")
}

android {
    namespace = "com.vigipro.core.ui"
}

dependencies {
    implementation(project(":core:core-model"))

    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.compose.material.icons.extended)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
}
