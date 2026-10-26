# Type-safe navigation routes are serialized with kotlinx.serialization.
-keep @kotlinx.serialization.Serializable class com.madmaxlgndklr.resonanceguide.** { *; }
-keepclassmembers class com.madmaxlgndklr.resonanceguide.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
