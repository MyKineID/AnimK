# R8 / ProGuard rules for Slf4j, Ktor, OkHttp, Jsoup, and Supabase

-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn org.jsoup.**
-dontwarn io.github.jan.**

-keep class org.slf4j.** { *; }
-keep class io.ktor.** { *; }
-keep class okhttp3.** { *; }
-keep class org.jsoup.** { *; }
-keep class io.github.jan.** { *; }

# Keep Kotlinx Serialization data models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class kotlinx.serialization.** { *; }
