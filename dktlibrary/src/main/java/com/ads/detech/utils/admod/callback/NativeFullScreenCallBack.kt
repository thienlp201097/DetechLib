package com.ads.detech.utils.admod.callback

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.nativead.NativeAd

interface NativeFullScreenCallBack {
    fun onLoaded(nativeAd: NativeAd)
    fun onLoadFailed()
}