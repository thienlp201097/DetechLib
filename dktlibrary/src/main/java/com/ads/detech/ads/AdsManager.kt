package com.ads.detech.ads

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.ads.detech.AdmobUtils
import com.ads.detech.AdmobUtils.isShowAds
import com.ads.detech.AdmobUtils.isTestDevice
import com.ads.detech.firebase.adsbn.AdsBannerNativeConfig
import com.ads.detech.firebase.inter.AdsInterConfig
import com.ads.detech.firebase.native_preload.AdsNativeConfig
import com.ads.detech.firebase.splash.AdsConfig
import com.ads.detech.utils.admod.NativeHolderAdmob
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.inVisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

object AdsManager {
    const val TAG = "==AdsManager=="

    fun showAdsSplash(activity: AppCompatActivity, key: String, viewGroup: ViewGroup , layout_native : Int, onAction: () -> Unit) {
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showAdsSplash: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            onAction()
            return
        }

        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsConfig::class.java)
            if (adsConfig.banner_splash == "1"){
                AdmobUtils.loadAdBanner(activity, adsConfig.units.banner, viewGroup, object :
                    AdmobUtils.BannerCallBack {
                    override fun onClickAds() {

                    }

                    override fun onFailed(message: String) {
                        checkAdsSplash(activity,adsConfig,layout_native,onAction)
                    }

                    override fun onLoad() {
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkAdsSplash(activity,adsConfig,layout_native,onAction)
                        }, 1500)
                    }

                    override fun onPaid(adValue: AdValue?, mAdView: AdView?) {
                    }
                })
            }
        }catch (_ : Exception){
            onAction()
        }
    }

    fun checkAdsSplash(activity: AppCompatActivity,adsConfig : AdsConfig,layout_native : Int,onAction: () -> Unit){
        when (adsConfig.ads_splash) {
            "1" -> AdsHolder.showAOA(activity, adsConfig.units.aoa, onAction)
            "2" -> AdsHolder.showInterstitial(activity, adsConfig.units.inter,false, onAction)
            "3" -> AdsHolder.showInterstitialWithNative(
                activity,
                adsConfig.units.inter,
                adsConfig.units.native,false,
                layout_native,
                onAction
            )
            else -> onAction()
        }
    }

    fun showAdsInterstitial(activity: AppCompatActivity, key: String, layout_native : Int, onAction: () -> Unit) {
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showAdsInterstitial: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            onAction()
            return
        }

        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsInterConfig::class.java)
            val displayInterval = adsConfig.count.toIntOrNull() ?: 1
            val shouldShow = AdsHolder.increaseAndCheck(key, displayInterval)

            if (shouldShow && displayInterval > 1) {
                onAction()
                return
            }

            when (adsConfig.type) {
                "1" -> AdsHolder.showInterstitial(activity, adsConfig.units.inter,true, onAction)
                "2" -> AdsHolder.showInterstitialWithNative(
                    activity,
                    adsConfig.units.inter,
                    adsConfig.units.native,true,
                    layout_native,
                    onAction
                )
                else -> onAction()
            }
        }catch (_ : Exception){
            onAction()
        }
    }

    fun showAdsBannerNative(activity: Activity, key: String, viewGroup: ViewGroup, layout_native : Int){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showAdsBannerNative: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            viewGroup.gone()
            return
        }
        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsBannerNativeConfig::class.java)

            when (adsConfig.ads_type) {
                "2" -> {
                    val bannerHolder = AdsHolder.getOrCreateBannerHolder(key, adsConfig.units.banner_collap)
                    AdsHolder.showAdBannerCollapsible(activity,bannerHolder,viewGroup)
                }
                "3" -> {
                    AdsHolder.NATIVE = NativeHolderAdmob(adsConfig.units.native)
                    AdsHolder.loadAndShowNative(activity,viewGroup,layout_native,adsConfig.native_type,AdsHolder.NATIVE)
                }
                "1" -> {
                    AdsHolder.showAdBanner(activity,adsConfig.units.banner,viewGroup)
                }

                else -> {
                    viewGroup.gone()
                }
            }
        }catch (_ : Exception){
            viewGroup.gone()
        }
    }

    fun preloadNative(activity: Activity, key: String){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "PreloadNative: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
            val nativeHolder = AdsHolder.getOrCreateNativeHolder(key, adsConfig.units.native)
            when (adsConfig.type) {
                "1" -> {
                    AdsHolder.loadNative(activity,nativeHolder)
                }
            }
        }catch (_ : Exception){

        }
    }

    fun preloadNativeFullScreen(activity: Activity, key: String,onFail: () -> Unit){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "preloadNativeFullScreen: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
            val nativeHolder = AdsHolder.getOrCreateNativeHolder(key, adsConfig.units.native)
            when (adsConfig.type) {
                "1" -> {
                    AdsHolder.loadNativeFullscreen(activity,nativeHolder){
                        onFail()
                    }
                }
                "0" ->{
                    onFail()
                }
            }
        }catch (_ : Exception){

        }
    }

    fun showNativeFullPreload(activity: Activity, key: String,viewGroup: ViewGroup,layout_native: Int){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showNativeFullPreload: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
            val nativeHolder = AdsHolder.getOrCreateNativeHolder(key, adsConfig.units.native)
            when (adsConfig.type) {
                "1" -> {
                    AdsHolder.showNativeFullscreen(activity,viewGroup,layout_native,nativeHolder)
                }

                else ->{
                    viewGroup.gone()
                }
            }
        }catch (_ : Exception){

        }
    }

    fun showNativePreload(activity: Activity, key: String,viewGroup: ViewGroup,layout_native: Int){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showNativePreload: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
            val nativeHolder = AdsHolder.getOrCreateNativeHolder(key, adsConfig.units.native)
            when (adsConfig.type) {
                "1" -> {
                    AdsHolder.showNative(activity,viewGroup,layout_native,nativeHolder)
                }

                else ->{
                    viewGroup.gone()
                }
            }
        }catch (_ : Exception){

        }
    }

    fun showNativeSmallPreload(activity: Activity, key: String,viewGroup: ViewGroup,layout_native: Int){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showNativeSmallPreload: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
            val nativeHolder = AdsHolder.getOrCreateNativeHolder(key, adsConfig.units.native)
            when (adsConfig.type) {
                "1" -> {
                    AdsHolder.showNativeSmall(activity,viewGroup,layout_native,nativeHolder)
                }

                else ->{
                    viewGroup.gone()
                }
            }
        }catch (_ : Exception){

        }
    }

    fun showNativeBannerPreload(activity: Activity, key: String,viewGroup: ViewGroup,layout_native: Int){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showNativeBannerPreload: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        try {
            val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
            val adsConfig = Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
            val nativeHolder = AdsHolder.getOrCreateNativeHolder(key, adsConfig.units.native)
            when (adsConfig.type) {
                "1" -> {
                    AdsHolder.showNativeSmallBanner(activity,viewGroup,layout_native,nativeHolder)
                }

                else ->{
                    viewGroup.gone()
                }
            }
        }catch (_ : Exception){

        }
    }
}