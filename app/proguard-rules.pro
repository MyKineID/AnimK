# R8 / ProGuard rules for Slf4j, Ktor, OkHttp, Jsoup, and Supabase

-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn org.jsoup.**
-dontwarn io.github.jan.**

# Do not keep whole dependency packages: R8 can strip their unused implementation.

# Keep Kotlinx Serialization data models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class kotlinx.serialization.** { *; }
