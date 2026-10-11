# Add project specific ProGuard rules here.
# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.emman.pomodoro.** {
    *** Companion;
}
-keepclasseswithmembers class com.emman.pomodoro.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if class com.emman.pomodoro.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowoptimization class <1>$$serializer { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
