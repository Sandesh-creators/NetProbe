# NetProbe ProGuard Rules
-keepattributes *Annotation*
-keep class com.netprobe.diagnostics.data.db.** { *; }
-keep class com.netprobe.diagnostics.data.model.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn javax.annotation.**
