# Nibras Nova Release R8 / ProGuard Rules
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# Keep Kotlinx Serialization generated serializers
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep @kotlinx.serialization.Serializable class com.nibras.nova.** { *; }
-keepclassmembers class com.nibras.nova.** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room Entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# WebView safety: Do not expose arbitrary classes, keep AndroidX WebKit compat
-keep class androidx.webkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.zxing.**

# TensorFlow Lite (Halal image filter)
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
