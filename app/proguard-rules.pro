# VigiPro ProGuard Rules

# ---- Supabase & Ktor ----
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ---- Kotlin Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep @Serializable classes
-keep,includedescriptorclasses class com.vigipro.core.model.**$$serializer { *; }
-keepclassmembers class com.vigipro.core.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.vigipro.core.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- ONVIF ----
-keep class com.seanproctor.onvifcamera.** { *; }

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ---- ML Kit Barcode ----
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ---- ZXing ----
-keep class com.google.zxing.** { *; }

# ---- Coil ----
-dontwarn coil3.**

# ---- libVLC ----
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.libvlc.**

# ---- General optimizations ----
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
