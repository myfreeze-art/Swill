# Add project specific ProGuard rules here.
# You can control the set of code used for each Android variant with
# proguardFiles.getDefaultProguardFile('proguard-android-optimize.txt')
# and the files listed under that main decompilation file

# Example for a Kotlin project
-keep class kotlin.Metadata { * }
-keep class kotlin.** { * }

# Keep R classes
-keep class **.R$* { *; }

# Keep native methods
-keep class * extends java.lang.Object {
    native <methods>;
}

# Keep JNI classes
-keep class com.swill.vpn.jni.** { *; }
