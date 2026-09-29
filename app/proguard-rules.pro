-keep class kotlin.Metadata { * }
-keep class kotlin.** { * }

-keep class **.R$* { *; }

-keep class * extends java.lang.Object {
    native <methods>;
}

-keep class com.swill.vpn.jni.** { *; }
