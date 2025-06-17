package com.lib.dktechads

import com.ads.detech.AppOpenManager
import com.ads.detech.application.AdsApplication

class MyApplication : AdsApplication() {
    override fun onCreateApplication() {

    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN){
            AppOpenManager.getInstance().timeToBackground = System.currentTimeMillis()
        }
    }
}