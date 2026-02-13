plugins {
    id("vigipro.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vigipro.core.model"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.kotlinx.serialization.json)
}
