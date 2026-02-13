plugins {
    id("vigipro.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vigipro.feature.auth"
}

dependencies {
    implementation(project(":core:core-network"))
}
