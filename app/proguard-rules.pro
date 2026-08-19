# ProGuard / R8 Rules for Run2Capture Production

# Preserve JavaScript Interface for Leaflet WebView Bridge
-keepclassmembers class com.example.feature.map.bridge.LeafletBridge$JavascriptInterfaceBridge {
    @android.webkit.JavascriptInterface <methods>;
}

-keepattributes JavascriptInterface
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization & Data Transfer Objects
-keepattributes Signature
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable class *;
}

# Retrofit / Moshi / JSON models
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Domain & Database Entities
-keep class com.example.core.database.entity.** { *; }
-keep class com.example.domain.model.** { *; }
-keep class com.example.core.network.model.** { *; }

