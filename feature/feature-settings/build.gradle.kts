plugins {
    id("vigipro.android.feature")
}

android {
    namespace = "com.vigipro.feature.settings"
}

dependencies {
    implementation(project(":core:core-network"))
}
