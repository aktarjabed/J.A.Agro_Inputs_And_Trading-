# INBusiness - Production ProGuard Rules

# Keep Hilt components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }

# Data classes
-keep class com.aktarjabed.inbusiness.data.entities.** { *; }
-keep class com.aktarjabed.inbusiness.domain.models.** { *; }

# Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}-dontwarn com.google.errorprone.annotations.**
# SQLCipher rules
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class org.sqlite.database.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
