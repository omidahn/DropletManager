# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Gson model classes (Retrofit + Gson reflection)
# Also preserve all annotation and generic metadata needed by Retrofit
-keepattributes Signature,
    Exceptions,
    InnerClasses,
    EnclosingMethod,
    RuntimeVisibleAnnotations,
    RuntimeInvisibleAnnotations,
    RuntimeVisibleParameterAnnotations,
    RuntimeInvisibleParameterAnnotations,
    AnnotationDefault,
    MethodParameters,
    SourceFile,
    LineNumberTable

# Keep data layer (Retrofit interfaces + DTOs + helpers + Gson models)
# Gson relies on field names, so ensure nothing under data.* is obfuscated.
-keep class com.omiddd.dropletmanager.data.** { *; }

# Common dontwarns
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# Retrofit/OkHttp common rules
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Keep Gson runtime classes and related internals used via reflection
# This prevents R8 from removing or renaming types Gson relies on
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-dontwarn com.google.gson.**

# Keep Gson TypeToken and reflective helpers (preserve generic signatures)
-keep class com.google.gson.reflect.TypeToken { *; }

# Keep fields annotated with @SerializedName used by Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName *;
}

# Keep kotlin metadata for Kotlin data classes
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.Metadata

# Keep anonymous or explicit subclasses of Gson TypeToken so generic parameter
# information used via getGenericSuperclass() / ParameterizedType is preserved.
# This is much narrower than keeping the entire application package.
-keep class * extends com.google.gson.reflect.TypeToken { *; }
