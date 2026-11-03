# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.sector6080.app.**$$serializer { *; }
-keepclassmembers class com.sector6080.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.sector6080.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
