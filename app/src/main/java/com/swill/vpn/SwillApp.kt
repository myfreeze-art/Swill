package com.swill.vpn

import android.app.Application
import android.content.Context
import com.swill.vpn.jni.NativeLoader

class SwillApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Load native libraries
        NativeLoader.load(this)
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Multi-dex if needed
    }
}
