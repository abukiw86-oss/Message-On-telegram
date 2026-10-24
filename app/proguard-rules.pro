# --- ProGuard & R8 Optimization Rules for Message on Telegram ---

# Code Optimization passes
-repackageclasses ''
-allowaccessmodification

# Strip verbose and debug logging in release build for performance and privacy
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Keep our core models and database entities
-keep class com.example.messageontelegram.core.network.** { *; }
-keep class com.example.messageontelegram.core.database.** { *; }
-keep class com.example.messageontelegram.core.preferences.** { *; }

# WorkManager Workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.work.impl.foreground.SystemForegroundService { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Jetpack Compose Runtime
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**
