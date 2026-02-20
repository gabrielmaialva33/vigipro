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
        buildConfigField("String", "SUPABASE_URL", "\"https://uvoanrknmoonudxyccwk.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV2b2FucmtubW9vbnVkeHljY3drIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE1NTQxNDIsImV4cCI6MjA4NzEzMDE0Mn0.FvDQ-nrAqmQjJSWHaDK5PbzBtb888SJtuS1I5BebJlQ\"")
    }
}

dependencies {
    implementation(project(":core:core-model"))

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Supabase (api — exposed to consumers like core-data)
    api(libs.supabase.auth)
    api(libs.supabase.postgrest)
    api(libs.supabase.realtime)
    api(libs.supabase.storage)
}
