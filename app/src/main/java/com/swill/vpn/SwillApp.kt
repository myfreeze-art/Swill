package com.swill.vpn

import android.app.Application
import android.content.Context
import com.swill.vpn.jni.NativeLoader

class SwillApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NativeLoader.load(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
}
