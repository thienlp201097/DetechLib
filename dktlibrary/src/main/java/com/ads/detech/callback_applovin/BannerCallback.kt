package com.ads.detech.callback_applovin

import com.applovin.mediation.MaxAd

interface BannerCallback {
    fun onBannerLoadFail(error:String)
    fun onBannerShowSucceed()
    fun onAdRevenuePaid(ad: MaxAd)
}