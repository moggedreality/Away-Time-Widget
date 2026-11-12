# ProGuard Rules for Away Time
# Enable in build.gradle.kts when ready: isMinifyEnabled = true

# Keep widget provider
-keep class com.stxrs.awaytime.AwayTimeWidget {
    *;
}

# Keep boot receiver
-keep class com.stxrs.awaytime.BootReceiver {
    *;
}

# Keep main activity
-keep class com.stxrs.awaytime.MainActivity {
    *;
}

# Keep data classes (settings, themes, etc.) for serialization
-keepclassmembers class com.stxrs.awaytime.** {
    <fields>;
}

# Keep Kotlin reflection for data classes
-keep class kotlin.Metadata { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep usage stats related classes
-keep class com.stxrs.awaytime.AwayTimeCalculator { *; }
-keep class com.stxrs.awaytime.WidgetSettings { *; }
-keep class com.stxrs.awaytime.Themes { *; }
-keep class com.stxrs.awaytime.WidgetRenderer { *; }

# Suppress warnings for unused code (we use reflection for some things)
-dontwarn com.stxrs.awaytime.**

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature

# Optimize
-optimizations !code/simplification/arithmetic

# Assume values may be null (Kotlin)
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNullParameter(...);
    static void checkNotNullExpressionValue(...);
}