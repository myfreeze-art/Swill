package com.swill.vpn.jni

import android.content.Context

object NativeLoader {

    private val loadedLibraries = mutableSetOf<String>()

    fun load(context: Context) {
        try {
            System.loadLibrary("native-lib")
            loadedLibraries.add("native-lib")

            try {
                System.loadLibrary("xray-jni")
                loadedLibraries.add("xray-jni")
            } catch (_: UnsatisfiedLinkError) {}

            try {
                System.loadLibrary("singbox-jni")
                loadedLibraries.add("singbox-jni")
            } catch (_: UnsatisfiedLinkError) {}

        } catch (_: Exception) {}
    }

    fun isLoaded(libName: String): Boolean = loadedLibraries.contains(libName)

    fun getLoadedLibraries(): Set<String> = loadedLibraries.toSet()
}
