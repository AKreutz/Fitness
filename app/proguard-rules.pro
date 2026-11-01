# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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
#-renamesourcefile

# Garmin Connect IQ SDK: its classes are parcelled in broadcasts from the Garmin Connect app and
# looked up by their original name (e.g. IQDevice), so R8 must not rename or strip them.
-keep class com.garmin.android.connectiq.** { *; }
-keep interface com.garmin.android.connectiq.** { *; }
-dontwarn com.garmin.android.connectiq.**
