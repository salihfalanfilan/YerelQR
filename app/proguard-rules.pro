# Add project specific ProGuard rules here.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Domain models: used by kotlinx.serialization (backup import/export), Room converters and enum valueOf()
-keep class com.example.offlineqr.domain.model.** { *; }
-keep class com.example.offlineqr.data.entity.** { *; }

# Type-safe navigation routes (serialized by Navigation Compose)
-keep class com.example.offlineqr.ui.navigation.** { *; }

# kotlinx.serialization generated serializers
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    *** Companion;
}

# ZXing
-dontwarn com.google.zxing.**
