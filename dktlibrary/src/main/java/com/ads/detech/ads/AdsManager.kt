package com.ads.detech.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.ads.detech.AdmobUtils
import com.ads.detech.AdmobUtils.isShowAds
import com.ads.detech.AdmobUtils.isTestDevice
import com.ads.detech.GoogleENative
import com.ads.detech.R
import com.ads.detech.firebase.adsbn.AdsBannerNativeConfig
import com.ads.detech.firebase.inter.AdsInterConfig
import com.ads.detech.firebase.native_preload.AdsNativeConfig
import com.ads.detech.firebase.splash.AdsConfig
import com.ads.detech.utils.admod.NativeHolderAdmob
import com.applovin.sdk.AppLovinSdkUtils
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
    val DEFAULT_LAYOUT_MEDIUM = R.layout.ad_template_medium
    val DEFAULT_LAYOUT_SMALL = R.layout.ad_template_small
    val DEFAULT_LAYOUT_BANNER = R.layout.ad_template_smallest
    val DEFAULT_LAYOUT_COLLAPSIBLE = R.layout.ad_template_mediumcollapsible

    private var nativeLayouts: List<Int> = emptyList()

    fun initNativeLayouts(layouts: List<Int>) {
        nativeLayouts = layouts
    }
    const val TAG = "==AdsManager=="

    fun showAdsSplash(activity: AppCompatActivity, key: String, viewGroup: ViewGroup, layout_native_full : Int, onAction: () -> Unit) {
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showAdsSplash: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            onAction()
            return
        }

        val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
        if (jsonStr.isBlank()) {
            Log.w("showAdsInterstitial", "⚠️ Không tìm thấy cấu hình cho key: $key")
            return
        }
        val adsConfig = try {
            Gson().fromJson(jsonStr, AdsConfig::class.java)
        } catch (e: Exception) {
            Log.e("showAdsInterstitial", "❌ Lỗi parse JSON: $jsonStr", e)
            return
        }
        if (adsConfig.banner_splash == "1"){
            AdmobUtils.loadAdBanner(activity, adsConfig.units.banner, viewGroup, object :
                AdmobUtils.BannerCallBack {
                override fun onClickAds() {

                }

                override fun onFailed(message: String) {
                    checkAdsSplash(activity,adsConfig,layout_native_full,onAction)
                }

                override fun onLoad() {
                    Handler(Looper.getMainLooper()).postDelayed({
                        checkAdsSplash(activity,adsConfig,layout_native_full,onAction)
                    }, 1500)
                }

                override fun onPaid(adValue: AdValue?, mAdView: AdView?) {
                }
            })
        }else{
            checkAdsSplash(activity,adsConfig,layout_native_full,onAction)
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
        val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
        if (jsonStr.isBlank()) {
            Log.w("showAdsInterstitial", "⚠️ Không tìm thấy cấu hình cho key: $key")
            return
        }
        val adsConfig = try {
            Gson().fromJson(jsonStr, AdsInterConfig::class.java)
        } catch (e: Exception) {
            Log.e("showAdsInterstitial", "❌ Lỗi parse JSON: $jsonStr", e)
            return
        }

        val displayInterval = adsConfig.count.toIntOrNull() ?: 1
        val shouldShow = AdsHolder.increaseAndCheck(key, displayInterval)

        if (shouldShow && adsConfig.count != "1") {
            Log.e("showAdsInterstitial", "Dãn cách hiển thị qc")
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


    }

    fun showAdsBannerNative(activity: Activity, key: String, viewGroup: ViewGroup){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "showAdsBannerNative: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            viewGroup.gone()
            return
        }
        val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
        if (jsonStr.isBlank()) {
            Log.w("showAdsBannerNative", "⚠️ Không tìm thấy cấu hình cho key: $key")
            return
        }
        val adsConfig = try {
            Gson().fromJson(jsonStr, AdsBannerNativeConfig::class.java)
        } catch (e: Exception) {
            Log.e("showAdsBannerNative", "❌ Lỗi parse JSON: $jsonStr", e)
            return
        }

        when (adsConfig.ads_type) {
            "1" -> {
                AdsHolder.showAdBanner(activity,adsConfig.units.banner,viewGroup)
            }

            "2" -> {
                val bannerHolder = AdsHolder.getOrCreateBannerHolder(key, adsConfig.units.banner_collap)
                AdsHolder.showAdBannerCollapsible(activity,bannerHolder,viewGroup)
            }
            "3" -> {
                AdsHolder.NATIVE = NativeHolderAdmob(adsConfig.units.native)
                val layoutId = when (adsConfig.native_type) {
                    "1" -> nativeLayouts.getOrNull(0) ?: DEFAULT_LAYOUT_MEDIUM
                    "2" -> nativeLayouts.getOrNull(1) ?: DEFAULT_LAYOUT_SMALL
                    "3" -> nativeLayouts.getOrNull(2) ?: DEFAULT_LAYOUT_BANNER
                    "4" -> nativeLayouts.getOrNull(3) ?: DEFAULT_LAYOUT_COLLAPSIBLE
                    else -> DEFAULT_LAYOUT_MEDIUM
                }
                when(adsConfig.native_type){
                    "1" ->{
                        AdsHolder.loadAndShowNative(activity,viewGroup,layoutId,GoogleENative.UNIFIED_MEDIUM,AdsHolder.NATIVE)
                    }
                    "2" ->{
                        AdsHolder.loadAndShowNative(activity,viewGroup,layoutId,GoogleENative.UNIFIED_SMALL,AdsHolder.NATIVE)
                    }
                    "3" ->{
                        AdsHolder.loadAndShowNative(activity,viewGroup,layoutId,GoogleENative.UNIFIED_BANNER,AdsHolder.NATIVE)
                    }
                    "4" ->{
                        val params: ViewGroup.LayoutParams = viewGroup.layoutParams
                        params.height = AppLovinSdkUtils.dpToPx(activity, 230)
                        viewGroup.layoutParams = params
                        AdsHolder.loadAndShowNativeCollapsible(activity,viewGroup,layoutId,GoogleENative.UNIFIED_MEDIUM,AdsHolder.NATIVE,
                            onLoaded = {
                                viewGroup.layoutParams = viewGroup.layoutParams.apply {
                                    height = AppLovinSdkUtils.dpToPx(activity, 330)
                                }
                            },
                            onClose = {
                                viewGroup.layoutParams = viewGroup.layoutParams.apply {
                                    height = AppLovinSdkUtils.dpToPx(activity, 80)
                                }
                            })
                    }
                    else -> {
                        AdsHolder.loadAndShowNative(activity,viewGroup,nativeLayouts.getOrNull(1) ?: DEFAULT_LAYOUT_SMALL,GoogleENative.UNIFIED_SMALL,AdsHolder.NATIVE)
                    }
                }
            }

            else -> {
                viewGroup.gone()
            }
        }
    }

    fun preloadNative(activity: Activity, key: String){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "PreloadNative: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
        if (jsonStr.isBlank()) {
            Log.w("PreloadNative", "⚠️ Không tìm thấy cấu hình cho key: $key")
            return
        }
        val adsConfig = try {
            Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
        } catch (e: Exception) {
            Log.e("PreloadNative", "❌ Lỗi parse JSON: $jsonStr", e)
            return
        }
        val nativeHolder = AdsHolder.getOrCreateNativeHolder(key, adsConfig.units.native)
        if (adsConfig.type == "0"){
            Log.d(TAG, "showNativePreload: Native $key Off")
            return
        }
        AdsHolder.loadNative(activity,nativeHolder)
    }

    fun showNativePreload(activity: Activity, key: String,viewGroup: ViewGroup){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            viewGroup.gone()
            Log.d(TAG, "showNativePreload: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
        if (jsonStr.isBlank()) {
            Log.w("showNativePreload", "⚠️ Không tìm thấy cấu hình cho key: $key")
            return
        }
        val adsConfig = try {
            Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
        } catch (e: Exception) {
            Log.e("showNativePreload", "❌ Lỗi parse JSON: $jsonStr", e)
            return
        }
        val nativeHolder = AdsHolder.getOrCreateNativeHolder(key, adsConfig.units.native)
        if (adsConfig.type == "0"){
            Log.d(TAG, "showNativePreload: Native $key Off")
            viewGroup.gone()
            return
        }
        val layoutId = when (adsConfig.type) {
            "1" -> nativeLayouts.getOrNull(0) ?: DEFAULT_LAYOUT_MEDIUM
            "2" -> nativeLayouts.getOrNull(1) ?: DEFAULT_LAYOUT_SMALL
            "3" -> nativeLayouts.getOrNull(2) ?: DEFAULT_LAYOUT_BANNER
            "4" -> nativeLayouts.getOrNull(3) ?: DEFAULT_LAYOUT_COLLAPSIBLE
            else -> DEFAULT_LAYOUT_MEDIUM
        }
        AdsHolder.showNative(activity,viewGroup,layoutId,nativeHolder)
    }

    fun preloadNativeFullScreen(activity: Activity, key: String,onFail: () -> Unit){
        if (isTestDevice || !AdmobUtils.isNetworkConnected(activity) || !isShowAds) {
            Log.d(TAG, "preloadNativeFullScreen: Bỏ qua quảng cáo (Test Device hoặc Không có mạng hoặc tắt quảng cáo)")
            return
        }
        val jsonStr = FirebaseRemoteConfig.getInstance().getString(key)
        if (jsonStr.isBlank()) {
            Log.w("preloadNativeFullScreen", "⚠️ Không tìm thấy cấu hình cho key: $key")
            return
        }
        val adsConfig = try {
            Gson().fromJson(jsonStr, AdsNativeConfig::class.java)
        } catch (e: Exception) {
            Log.e("preloadNativeFullScreen", "❌ Lỗi parse JSON: $jsonStr", e)
            return
        }
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
}