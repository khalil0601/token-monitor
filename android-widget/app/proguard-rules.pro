# Token Monitor Widget ProGuard Rules
-keepattributes *Annotation*
-keep class com.tokenmonitor.widget.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
