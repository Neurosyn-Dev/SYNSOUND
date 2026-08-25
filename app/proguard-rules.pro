# SynSound ProGuard Rules
-keepattributes *Annotation*
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep AndroidX WebKit classes
-keep class androidx.webkit.** { *; }

# Keep Material & AppCompat components
-keep class com.google.android.material.** { *; }
-keep class androidx.appcompat.** { *; }

# Optimize code while keeping Web Chrome & Web View safe
-dontwarn android.webkit.**
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}
