# Room and Hilt generate code that R8 keeps on its own via consumer rules.
# Navigation Compose type-safe routes are kotlinx.serialization @Serializable
# classes, whose generated serializers must survive minification (SPEC 6, phase 7).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.oleksandr.fastflow.** {
    *** Companion;
}
-keepclasseswithmembers class com.oleksandr.fastflow.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Glance widget receivers are instantiated by the framework by name.
-keep class com.oleksandr.fastflow.widget.** { *; }
