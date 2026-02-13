plugins {
    id("vigipro.android.library")
    id("vigipro.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vigipro.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // TODO: Replace with actual Supabase project values
        buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"your-anon-key\"")
    }
}

dependencies {
    implementation(project(":core:core-model"))

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Supabase
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
}
