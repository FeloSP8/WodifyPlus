# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all Compose classes
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class * extends androidx.room.RoomDatabase$Callback

# Keep Chaquopy Python classes
-keep class com.chaquo.python.** { *; }
-keep class com.chaquo.python.android.** { *; }

# Keep WorkManager classes
-keep class androidx.work.** { *; }

# Keep notification classes
-keep class androidx.core.app.NotificationCompat** { *; }

# Keep widget classes
-keep class * extends android.appwidget.AppWidgetProvider

# Keep data classes (models)
-keep class com.example.wodifyplus.data.models.** { *; }
-keep class com.example.wodifyplus.data.local.entities.** { *; }

# Keep JSON parsing
-keep class org.json.** { *; }

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep all public methods in main package
-keep class com.example.wodifyplus.** { public *; }