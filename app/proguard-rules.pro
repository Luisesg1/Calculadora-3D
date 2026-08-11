-keepattributes *Annotation*, Signature, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.print3d.calculator.**$$serializer { *; }
-keepclassmembers class com.print3d.calculator.** { *** Companion; }
-keepclasseswithmembers class com.print3d.calculator.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor/Supabase pull an optional slf4j binding that isn't on the classpath — safe to ignore.
-dontwarn org.slf4j.**
