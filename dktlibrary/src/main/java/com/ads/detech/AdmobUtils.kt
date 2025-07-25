package com.ads.detech

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.detech.NativeFunc.Companion.populateNativeAdView
import com.ads.detech.NativeFunc.Companion.populateNativeAdViewClose
import com.ads.detech.NativeFunc.Companion.populateNativeAdViewNoBtn
import com.ads.detech.utils.SweetAlert.SweetAlertDialog
import com.ads.detech.utils.admod.BannerHolderAdmob
import com.ads.detech.utils.admod.InterHolderAdmob
import com.ads.detech.utils.admod.NativeHolderAdmob
import com.ads.detech.utils.admod.RewardHolderAdmob
import com.ads.detech.utils.admod.RewardedInterstitialHolderAdmob
import com.ads.detech.utils.admod.callback.AdCallBackInterLoad
import com.ads.detech.utils.admod.callback.AdLoadCallback
import com.ads.detech.utils.admod.callback.AdsInterCallBack
import com.ads.detech.utils.admod.callback.MobileAdsListener
import com.ads.detech.utils.admod.callback.NativeAdmobCallback
import com.ads.detech.utils.admod.callback.NativeFullScreenCallBack
import com.ads.detech.utils.admod.callback.RewardAdCallback
import com.ads.detech.utils.admod.remote.BannerPlugin
import com.airbnb.lottie.LottieAnimationView
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.vapp.admoblibrary.ads.remote.BannerRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Date
import java.util.Locale

object AdmobUtils {
    //Dialog loading
    @SuppressLint("StaticFieldLeak")
    @JvmField
    var dialog: SweetAlertDialog? = null
    var dialogFullScreen: Dialog? = null

    // Biến check lần cuối hiển thị quảng cáo
    var lastTimeShowInterstitial: Long = 0
    // Timeout init admob
    var timeOut = 0
    //check ADS test
    var isTestDevice = false
    var isCheckTestDevice = false
    //Check quảng cáo đang show hay không
    @JvmField
    var isAdShowing = false
    var isClick = false

    //Ẩn hiện quảng cáo
    @JvmField
    var isShowAds = true

    //Dùng ID Test để hiển thị quảng cáo
    @JvmField
    var isTesting = false

    //List device test
    var testDevices: MutableList<String> = ArrayList()
    var deviceId = ""

    //Reward Ads
    @JvmField
    var mRewardedAd: RewardedAd? = null
    var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    var mInterstitialAd: InterstitialAd? = null
    var shimmerFrameLayout: ShimmerFrameLayout? = null

    //id thật
    var idIntersitialReal: String? = null
    var interIsShowingWithNative = false
    var interIsShowingWithBanner = false

    //Hàm Khởi tạo admob
    @JvmStatic
    fun initAdmob(
        context: Context,
        isDebug: Boolean,
        isEnableAds: Boolean,
        isCheckTestDevice : Boolean,
        mobileAdsListener: MobileAdsListener
    ) {
        this.isCheckTestDevice = isCheckTestDevice
        isTesting = isDebug
        isShowAds = isEnableAds
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(context) {}
            withContext(Dispatchers.Main){
                mobileAdsListener.onSuccess()
            }
        }
    }

    fun adImpressionFacebookSDK(context: Context, ad: AdValue) {
        val logger = AppEventsLogger.newLogger(context)
        val params = Bundle()
        params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, ad.currencyCode)
        logger.logEvent(
            AppEventsConstants.EVENT_NAME_AD_IMPRESSION, ad.valueMicros / 1000000.0, params
        )
    }

    fun initListIdTest() {
        testDevices.add("D4A597237D12FDEC52BE6B2F15508BB")
    }

    //check open network
    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        } catch (e: Exception) {
            return false
        }
    }

    interface BannerCallBack {
        fun onClickAds()
        fun onLoad()
        fun onFailed(message: String)
        fun onPaid(adValue: AdValue?, mAdView: AdView?)
    }

    @JvmStatic
    fun loadAdBanner(
        activity: Activity,
        bannerId: String?,
        viewGroup: ViewGroup,
        bannerAdCallback: BannerCallBack
    ) {

        if (isTestDevice){
            bannerAdCallback.onFailed("None Show")
            return
        }
        var bannerId = bannerId
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            bannerAdCallback.onFailed("None Show")
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_id)
        }
        mAdView.adUnitId = bannerId!!
        val adSize = getAdSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        val adRequest = AdRequest.Builder().build()

        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        mAdView.onPaidEventListener =
            OnPaidEventListener { adValue ->
                adImpressionFacebookSDK(activity,adValue)
            }
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onLoad()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onFailed(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                bannerAdCallback.onClickAds()
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        }
        mAdView.loadAd(adRequest)
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadAdBannerWithAdsSize(
        activity: Activity,
        bannerId: String?,
        viewGroup: ViewGroup,adSize: AdSize,
        bannerAdCallback: BannerCallBack
    ) {

        if (isTestDevice){
            bannerAdCallback.onFailed("None Show")
            return
        }
        var bannerId = bannerId
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            bannerAdCallback.onFailed("None Show")
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_id)
        }
        mAdView.adUnitId = bannerId!!
        mAdView.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()

        try {
            viewGroup.addView(mAdView)
        } catch (_: Exception) {

        }
        mAdView.onPaidEventListener =
            OnPaidEventListener { adValue ->
                adImpressionFacebookSDK(activity,adValue)
            }
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                bannerAdCallback.onLoad()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                bannerAdCallback.onFailed(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                bannerAdCallback.onClickAds()
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        }
        mAdView.loadAd(adRequest)
        Log.e(" Admod", "loadAdBanner")
    }

    interface BannerCollapsibleAdCallback {
        fun onClickAds()
        fun onBannerAdLoaded(adSize: AdSize)
        fun onAdFail(message: String)
        fun onAdPaid(adValue: AdValue, mAdView: AdView)
    }

    var mAdView: AdView? = null

    @JvmStatic
    fun loadAdBannerCollapsibleReload(
        activity: Activity,
        banner: BannerHolderAdmob,
        collapsibleBannerSize: CollapsibleBanner,
        viewGroup: ViewGroup,
        callback: BannerCollapsibleAdCallback
    ) {
        if (isTestDevice){
            callback.onAdFail("is Test Device")
            return
        }
        var bannerId = banner.ads
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        banner.mAdView?.destroy()
        banner.mAdView?.let {
            viewGroup.removeView(it)
        }
        banner.mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        banner.mAdView?.adUnitId = bannerId
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(banner.mAdView, 1)
        } catch (_: Exception) {

        }
        val adSize = getAdSize(activity)
        banner.mAdView?.setAdSize(adSize)
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        banner.mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                banner.mAdView?.onPaidEventListener =
                    OnPaidEventListener { adValue ->
                        adImpressionFacebookSDK(activity,adValue)
                    }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerAdLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onAdFail(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                callback.onClickAds()
            }

            override fun onAdClosed() {

            }
        }
        val extras = Bundle()
        var anchored = "top"
        anchored = if (collapsibleBannerSize === CollapsibleBanner.TOP) {
            "top"
        } else {
            "bottom"
        }
        extras.putString("collapsible", anchored)
        val adRequest2 =
            AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        banner.mAdView?.loadAd(adRequest2)
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadAdBannerCollapsible(
        activity: Activity,
        bannerId: String?,
        collapsibleBannerSize: CollapsibleBanner,
        viewGroup: ViewGroup,
        callback: BannerCollapsibleAdCallback
    ) {
        if (isTestDevice){
            callback.onAdFail("is Test Device")
            return
        }
        var bannerId = bannerId
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        mAdView.adUnitId = bannerId!!
        val adSize = getAdSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                mAdView.onPaidEventListener =
                    OnPaidEventListener { adValue ->

                    }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerAdLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onAdFail(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                callback.onClickAds()
            }

            override fun onAdClosed() {

            }
        }
        val extras = Bundle()
        var anchored = "top"
        anchored = if (collapsibleBannerSize === CollapsibleBanner.TOP) {
            "top"
        } else {
            "bottom"
        }
        extras.putString("collapsible", anchored)
        val adRequest2 =
            AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        mAdView.loadAd(adRequest2)
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadAndShowBannerCollapsibleWithConfig(
        activity: Activity,
        id: String,
        refreshRateSec: Int,
        cbFetchIntervalSec: Int,
        view: ViewGroup,
        size: GoogleEBanner,
        bannerAdCallback: BannerCollapsibleAdCallback
    ) {
        if (isTestDevice){
            bannerAdCallback.onAdFail("is Test Device")
            return
        }
        var bannerPlugin: BannerPlugin? = null
        val type = if (size == GoogleEBanner.UNIFIED_TOP) {
            "collapsible_top"
        } else {
            "collapsible_bottom"
        }
        val bannerConfig = BannerPlugin.BannerConfig(id, type, refreshRateSec, cbFetchIntervalSec)
        bannerPlugin = bannerConfig.adUnitId?.let {
            BannerPlugin(
                activity, view, it, bannerConfig, object : BannerRemoteConfig {
                    override fun onBannerAdLoaded(adSize: AdSize?) {
                        adSize?.let { it1 -> bannerAdCallback.onBannerAdLoaded(it1) }
                    }

                    override fun onAdFail() {
                        Log.d("===Banner", "Banner2")
                        bannerAdCallback.onAdFail("Banner Failed")
                    }

                    override fun onAdPaid(adValue: AdValue, mAdView: AdView) {
                    }
                })
        }
    }

    @JvmStatic
    fun loadAndShowBannerWithConfig(
        activity: Activity,
        id: String, refreshRateSec: Int, cbFetchIntervalSec: Int, view: ViewGroup, size: String,
        bannerAdCallback: BannerCollapsibleAdCallback
    ) {
        if (isTestDevice){
            bannerAdCallback.onAdFail("is Test Device")
            return
        }
        var bannerPlugin: BannerPlugin? = null
        val bannerConfig = BannerPlugin.BannerConfig(id, size, refreshRateSec, cbFetchIntervalSec)
        bannerPlugin = bannerConfig.adUnitId?.let {
            BannerPlugin(
                activity, view, it, bannerConfig, object : BannerRemoteConfig {
                    override fun onBannerAdLoaded(adSize: AdSize?) {
                        adSize?.let { it1 -> bannerAdCallback.onBannerAdLoaded(it1) }
                        shimmerFrameLayout?.stopShimmer()
                        Log.d("===Banner==", "reload banner")
                    }

                    override fun onAdFail() {
                        Log.d("===Banner", "Banner2")
                        shimmerFrameLayout?.stopShimmer()
                        bannerAdCallback.onAdFail("Banner Failed")
                    }

                    override fun onAdPaid(adValue: AdValue, mAdView: AdView) {
                    }
                })
        }
    }


    private fun getAdSize(context: Activity): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = context.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    /**
     * Load and Show Native
     *
     *
     *
     */

    @JvmStatic
    fun loadAndGetNativeAds(
        context: Context,
        nativeHolder: NativeHolderAdmob,
        adCallback: NativeAdmobCallback
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onAdFail("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        val adRequest = AdRequest.Builder().build()
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_ads_admob_native_id)
        }
        nativeHolder.isLoad = true
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).build()
        val adLoader: AdLoader = AdLoader.Builder(context, nativeHolder.ads)
            .forNativeAd { nativeAd ->
                nativeHolder.nativeAd = nativeAd
                nativeHolder.isLoad = false
                nativeHolder.native_mutable.value = nativeAd
                checkAdsTest(nativeAd)
                nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                    adValue?.let {
                        adCallback.onPaid(adValue, nativeHolder.ads)
                        adImpressionFacebookSDK(context,it)
                    }
                }
                adCallback.onLoadedAndGetNativeAd(nativeAd)
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    nativeHolder.nativeAd = null
                    nativeHolder.isLoad = false
                    nativeHolder.native_mutable.value = null
                    adCallback.onAdFail(adError.message)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adLoader.loadAd(adRequest)

    }

    //Load native 2 in here
    interface AdsNativeCallBackAdmod {
        fun NativeLoaded()
        fun NativeFailed(massage: String)
        fun onPaid(adValue: AdValue?, adUnitAds: String?)
    }

    @JvmStatic
    fun showNativeAdsWithLayout(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        callback: AdsNativeCallBackAdmod
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            callback.NativeFailed("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }

        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeHolder.nativeAd!!, adView, size)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                try {
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                callback.NativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.NativeFailed("None Show")
            }
        } else {
            val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else if (size === GoogleENative.UNIFIED_SMALL) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
            }
            try {
                viewGroup.addView(tagView, 0)
            } catch (_: Exception) {

            }

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        adImpressionFacebookSDK(activity,it)
                        callback.onPaid(it, nativeHolder.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(nativeAd, adView, size)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }

                    callback.NativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.NativeFailed("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    @JvmStatic
    fun showNativeAdsWithLayoutCollapsible(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        callback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            callback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }

        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdViewClose(nativeHolder.nativeAd!!, adView, size,callback)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                try {
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                callback.onNativeAdLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.onAdFail("None Show")
            }
        } else {
            val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else if (size === GoogleENative.UNIFIED_SMALL) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
            }
            try {
                viewGroup.addView(tagView, 0)
            } catch (_: Exception) {

            }

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        adImpressionFacebookSDK(activity,it)
                        callback.onAdPaid(it, nativeHolder.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdViewClose(nativeAd, adView, size,callback)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }

                    callback.onNativeAdLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.onAdFail("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    // ads native
    interface NativeAdCallbackNew {
        fun onLoadedAndGetNativeAd(ad: NativeAd?)
        fun onNativeAdLoaded()
        fun onAdFail(error: String)
        fun onAdPaid(adValue: AdValue?, adUnitAds: String?)
        fun onClickAds()
    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAds(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }

        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        var s = nativeHolder.ads
        val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else if (size === GoogleENative.UNIFIED_SMALL) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        }

        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeAdLoaded()
                checkAdsTest(nativeAd)
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, size)
                shimmerFrameLayout.stopShimmer()
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adImpressionFacebookSDK(activity,adValue)
                    adCallback.onAdPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onAdFail(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onClickAds()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        val adRequest = AdRequest.Builder().build()
        adLoader.loadAd(adRequest)
        Log.e("Admod", "loadAdNativeAds")
    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAdsCollapsible(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        val adRequest = AdRequest.Builder().build()
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        var s = nativeHolder.ads
        val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        }
        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeAdLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdViewClose(nativeAd, adView, size, adCallback)
                shimmerFrameLayout.stopShimmer()
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adImpressionFacebookSDK(activity,adValue)
                    adCallback.onAdPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onAdFail(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adLoader.loadAd(adRequest)
    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAdsCollapsibleNoShimmer(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        val adRequest = AdRequest.Builder().build()
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        var s = nativeHolder.ads
        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeAdLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdViewClose(nativeAd, adView, GoogleENative.UNIFIED_MEDIUM, adCallback)
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adImpressionFacebookSDK(activity,adValue)
                    adCallback.onAdPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onAdFail(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adLoader.loadAd(adRequest)
    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAdsNoShimmer(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            viewGroup.visibility = View.GONE
            adCallback.onAdFail("is Test Device")
            return
        }
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        val adRequest = AdRequest.Builder().build()
        var s = nativeHolder.ads
        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeAdLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, size)
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onAdPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onAdFail(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onClickAds()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()

        adLoader.loadAd(adRequest)

        Log.e("Admod", "loadAdNativeAds")
    }

    /**
     * Load and Show Interstitial
     * Load and Show Interstitial
     * Load and Show Interstitial
     * Load and Show Interstitial
     */

    @JvmStatic
    fun loadAndShowAdInterstitial(
        activity: AppCompatActivity,
        admobHolder: InterHolderAdmob,
        adCallback: AdsInterCallBack,
        enableLoadingDialog: Boolean
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        var admobId = admobHolder.ads
        mInterstitialAd = null
        isAdShowing = false

        val appOpenManager = AppOpenManager.getInstance()

        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback.onAdFail("No internet")
            return
        }

        if (appOpenManager.isInitialized && !appOpenManager.isAppResumeEnabled) {
            return
        } else if (appOpenManager.isInitialized) {
            appOpenManager.isAppResumeEnabled = false
        }

        if (enableLoadingDialog) {
            dialogLoading(activity)
        }

        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_id)
        } else {
            checkIdTest(activity, admobId)
        }

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(activity, admobId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                adCallback.onAdLoaded()

                Handler(Looper.getMainLooper()).postDelayed({
                    mInterstitialAd = interstitialAd

                    mInterstitialAd?.apply {
                        onPaidEventListener = OnPaidEventListener { adValue ->
                            adImpressionFacebookSDK(activity, adValue)
                        }

                        fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                handleAdFailure(adError.message, adCallback, appOpenManager)
                            }

                            override fun onAdDismissedFullScreenContent() {
                                lastTimeShowInterstitial = Date().time
                                adCallback.onEventClickAdClosed()
                                cleanupAfterAd(appOpenManager)
                            }

                            override fun onAdShowedFullScreenContent() {
                                adCallback.onAdShowed()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    dismissAdDialog()
                                }, 800)
                            }

                            override fun onAdClicked() {
                                adCallback.onClickAds()
                            }
                        }

                        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            adCallback.onStartAction()
                            show(activity)
                            isAdShowing = true
                        } else {
                            handleAdFailure("Interstitial can't show in background", adCallback, appOpenManager)
                        }
                    } ?: run {
                        handleAdFailure("mInterstitialAd null", adCallback, appOpenManager)
                    }
                }, 800)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                handleAdFailure(loadAdError.message, adCallback, appOpenManager)
            }
        })
    }

    private fun handleAdFailure(
        message: String,
        adCallback: AdsInterCallBack,
        appOpenManager: AppOpenManager
    ) {
        mInterstitialAd = null
        isAdShowing = false
        if (appOpenManager.isInitialized) {
            appOpenManager.isAppResumeEnabled = true
        }
        dismissAdDialog()
        adCallback.onAdFail(message)
        Log.e("Admodfail", "Ad failed: $message")
    }

    private fun cleanupAfterAd(appOpenManager: AppOpenManager) {
        mInterstitialAd = null
        isAdShowing = false
        if (appOpenManager.isInitialized) {
            appOpenManager.isAppResumeEnabled = true
        }
    }

    //Load Inter in here
    @JvmStatic
    fun loadAndGetAdInterstitial(
        activity: Context,
        interHolder: InterHolderAdmob,
        adLoadCallback: AdCallBackInterLoad
    ) {
        if (isTestDevice){
            adLoadCallback.onAdFail("is Test Device")
            return
        }
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adLoadCallback.onAdFail("None Show")
            return
        }
        if (interHolder.inter != null) {
            Log.d("===AdsInter", "inter not null")
            return
        }
        interHolder.check = true
        val adRequest = AdRequest.Builder().build()
        if (isTesting) {
            interHolder.ads = activity.getString(R.string.test_ads_admob_inter_id)
        }
        idIntersitialReal = interHolder.ads
        InterstitialAd.load(
            activity,
            idIntersitialReal!!,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    if (isClick) {
                        interHolder.mutable.value = interstitialAd
                    }
                    interHolder.inter = interstitialAd
                    interHolder.check = false
                    interHolder.inter!!.setOnPaidEventListener { adValue ->

                        Log.d("==Advalue==", "onAdLoaded:  ${interHolder.inter!!.responseInfo}")
                        adLoadCallback.onPaid(adValue, interHolder.inter!!.adUnitId)
                    }
                    adLoadCallback.onAdLoaded(interstitialAd, false)
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdShowing = false
                    if (mInterstitialAd != null) {
                        mInterstitialAd = null
                    }
                    interHolder.check = false
                    if (isClick) {
                        interHolder.mutable.value = null
                    }
                    adLoadCallback.onAdFail(loadAdError.message)
                }
            })
    }

    //Load Inter 2 in here if inter 1 false

    //Show Inter in here
    @JvmStatic
    fun showAdInterstitialWithCallbackNotLoadNew(
        activity: Activity,
        interHolder: InterHolderAdmob,
        timeout: Long,
        adCallback: AdsInterCallBack?,
        enableLoadingDialog: Boolean
    ) {
        if (isTestDevice){
            adCallback?.onAdFail("is Test Device")
            return
        }
        isClick = true
        //Check internet
        if (!isShowAds || !isNetworkConnected(activity)) {
            isAdShowing = false
            if (AppOpenManager.getInstance().isInitialized) {
                AppOpenManager.getInstance().isAppResumeEnabled = true
            }
            adCallback?.onAdFail("No internet")
            return
        }
        adCallback?.onAdLoaded()
        val handler = Handler(Looper.getMainLooper())
        //Check timeout show inter
        val runnable = Runnable {
            if (interHolder.check) {
                if (AppOpenManager.getInstance().isInitialized) {
                    AppOpenManager.getInstance().isAppResumeEnabled = true
                }
                isClick = false
                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                isAdShowing = false
                dismissAdDialog()
                adCallback?.onAdFail("timeout")
            }
        }
        handler.postDelayed(runnable, timeout)
        //Inter is Loading...
        if (interHolder.check) {
            if (enableLoadingDialog) {
                dialogLoading(activity)
            }
            interHolder.mutable.observe((activity as LifecycleOwner)) { aBoolean: InterstitialAd? ->
                if (aBoolean != null) {
                    interHolder.mutable.removeObservers((activity as LifecycleOwner))
                    isClick = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("===DelayLoad", "delay")

                        aBoolean.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                isAdShowing = false
                                if (AppOpenManager.getInstance().isInitialized) {
                                    AppOpenManager.getInstance().isAppResumeEnabled = true
                                }
                                isClick = false
                                //Set inter = null
                                interHolder.inter = null
                                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                                interHolder.mutable.value = null
                                adCallback?.onEventClickAdClosed()
                                dismissAdDialog()
                                Log.d("TAG", "The ad was dismissed.")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                isAdShowing = false
                                if (AppOpenManager.getInstance().isInitialized) {
                                    AppOpenManager.getInstance().isAppResumeEnabled = true
                                }
                                isClick = false
                                isAdShowing = false
                                //Set inter = null
                                interHolder.inter = null
                                dismissAdDialog()
                                Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                                interHolder.mutable.value = null
                                handler.removeCallbacksAndMessages(null)
                                adCallback?.onAdFail(adError.message)
                            }

                            override fun onAdShowedFullScreenContent() {
                                val responseInfo = aBoolean.responseInfo
                                val mediationAdapter = responseInfo.mediationAdapterClassName
                                Log.d("AdMob", "Ad from: $mediationAdapter")

                                handler.removeCallbacksAndMessages(null)
                                isAdShowing = true
                                adCallback?.onAdShowed()

                            }

                            override fun onAdClicked() {
                                super.onAdClicked()
                                adCallback?.onClickAds()
                            }
                        }
                        showInterstitialAdNew(activity, aBoolean, adCallback)
                    }, 400)
                } else {
                    interHolder.check = true
                }
            }
            return
        }
        //Load inter done
        if (interHolder.inter == null) {
            if (adCallback != null) {
                isAdShowing = false
                if (AppOpenManager.getInstance().isInitialized) {
                    AppOpenManager.getInstance().isAppResumeEnabled = true
                }
                adCallback.onAdFail("inter null")
                handler.removeCallbacksAndMessages(null)
            }
        } else {
            if (enableLoadingDialog) {
                dialogLoading(activity)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                interHolder.inter?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            isAdShowing = false
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = true
                            }
                            isClick = false
                            interHolder.mutable.removeObservers((activity as LifecycleOwner))
                            interHolder.inter = null
                            adCallback?.onEventClickAdClosed()
                            dismissAdDialog()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isAdShowing = false
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = true
                            }
                            handler.removeCallbacksAndMessages(null)
                            isClick = false
                            interHolder.inter = null
                            interHolder.mutable.removeObservers((activity as LifecycleOwner))
                            isAdShowing = false
                            dismissAdDialog()
                            adCallback?.onAdFail(adError.message)
                            Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                            Log.e("Admodfail", "errorCodeAds" + adError.cause)
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            adCallback?.onClickAds()
                        }

                        override fun onAdShowedFullScreenContent() {
                            handler.removeCallbacksAndMessages(null)
                            isAdShowing = true
                            adCallback?.onAdShowed()
                        }
                    }
                showInterstitialAdNew(activity, interHolder.inter, adCallback)
            }, 400)
        }
    }

    @JvmStatic
    private fun showInterstitialAdNew(
        activity: Activity,
        mInterstitialAd: InterstitialAd?,
        callback: AdsInterCallBack?
    ) {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
            isAdShowing = true
            Handler(Looper.getMainLooper()).postDelayed({
                callback?.onStartAction()

                mInterstitialAd.show(activity)
            }, 400)
        } else {
            isAdShowing = false
            if (AppOpenManager.getInstance().isInitialized) {
                AppOpenManager.getInstance().isAppResumeEnabled = true
            }
            dismissAdDialog()
            callback?.onAdFail("onResume")
        }
    }

    @JvmStatic
    fun dismissAdDialog() {
        try {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
            }
            if (dialogFullScreen != null && dialogFullScreen?.isShowing == true) {
                dialogFullScreen?.dismiss()
            }
        } catch (_: Exception) {

        }
    }

    /**
     * Load and Show Reward
     * Load and Show Reward
     * Load and Show Reward
     * Load and Show Reward
     */

    @JvmStatic
    fun loadAndShowAdRewardWithCallback(
        activity: Activity,
        admobId: String?,
        adCallback2: RewardAdCallback,
        enableLoadingDialog: Boolean
    ) {
        if (isTestDevice){
            adCallback2.onAdFail("is Test Device")
            return
        }
        var admobId = admobId
        mInterstitialAd = null
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback2.onAdClosed()
            return
        }
        val adRequest = AdRequest.Builder().build()
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_reward_id)
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AppOpenManager.getInstance().isInitialized) {
            AppOpenManager.getInstance().isAppResumeEnabled = false
        }
        RewardedAd.load(activity, admobId!!,
            adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    mRewardedAd = null
                    adCallback2.onAdFail(loadAdError.message)
                    dismissAdDialog()
                    if (AppOpenManager.getInstance().isInitialized) {
                        AppOpenManager.getInstance().isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    Log.e("Admodfail", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("Admodfail", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    if (mRewardedAd != null) {
                        mRewardedAd?.setOnPaidEventListener {

                        }
                        mRewardedAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback2.onAdShowed()
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = false
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    // Called when ad fails to show.
                                    if (adError.code != 1) {
                                        isAdShowing = false
                                        adCallback2.onAdFail(adError.message)
                                        mRewardedAd = null
                                        dismissAdDialog()
                                    }
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = true
                                    }
                                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    mRewardedAd = null
                                    isAdShowing = false
                                    adCallback2.onAdClosed()
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = true
                                    }
                                }
                            }
                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = false
                            }
                            mRewardedAd?.show(activity) { adCallback2.onEarned() }
                            isAdShowing = true
                        } else {
                            mRewardedAd = null
                            dismissAdDialog()
                            isAdShowing = false
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = true
                            }
                        }
                    } else {
                        isAdShowing = false
                        adCallback2.onAdFail("None Show")
                        dismissAdDialog()
                        if (AppOpenManager.getInstance().isInitialized) {
                            AppOpenManager.getInstance().isAppResumeEnabled = true
                        }
                    }
                }
            })
    }


    /**
     * Load and Show RewardedInterstitial
     * Load and Show RewardedInterstitial
     * Load and Show RewardedInterstitial
     * Load and Show RewardedInterstitial
     */
    @JvmStatic
    fun loadAndShowRewardedInterstitialAdWithCallback(
        activity: Activity,
        admobId: String?,
        adCallback2: RewardAdCallback,
        enableLoadingDialog: Boolean
    ) {
        if (isTestDevice){
            adCallback2.onAdFail("is Test Device")
            return
        }
        var admobId = admobId
        mInterstitialAd = null
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback2.onAdClosed()
            return
        }
        val adRequest = AdRequest.Builder().build()
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_reward_id)
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AppOpenManager.getInstance().isInitialized) {
            AppOpenManager.getInstance().isAppResumeEnabled = false
        }
        RewardedInterstitialAd.load(activity, admobId!!,
            adRequest, object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    mRewardedInterstitialAd = null
                    adCallback2.onAdFail(loadAdError.message)
                    dismissAdDialog()
                    if (AppOpenManager.getInstance().isInitialized) {
                        AppOpenManager.getInstance().isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    Log.e("Admodfail", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("Admodfail", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                    mRewardedInterstitialAd = rewardedAd
                    if (mRewardedInterstitialAd != null) {
                        mRewardedInterstitialAd?.setOnPaidEventListener {

                        }
                        mRewardedInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback2.onAdShowed()
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = false
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    // Called when ad fails to show.
                                    if (adError.code != 1) {
                                        isAdShowing = false
                                        adCallback2.onAdFail(adError.message)
                                        mRewardedInterstitialAd = null
                                        dismissAdDialog()
                                    }
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = true
                                    }
                                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    mRewardedInterstitialAd = null
                                    isAdShowing = false
                                    adCallback2.onAdClosed()
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = true
                                    }
                                }
                            }
                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = false
                            }
                            mRewardedInterstitialAd?.show(activity) { adCallback2.onEarned() }
                            isAdShowing = true
                        } else {
                            mRewardedInterstitialAd = null
                            dismissAdDialog()
                            isAdShowing = false
                            if (AppOpenManager.getInstance().isInitialized) {
                                AppOpenManager.getInstance().isAppResumeEnabled = true
                            }
                        }
                    } else {
                        isAdShowing = false
                        adCallback2.onAdFail("None Show")
                        dismissAdDialog()
                        if (AppOpenManager.getInstance().isInitialized) {
                            AppOpenManager.getInstance().isAppResumeEnabled = true
                        }
                    }
                }
            })
    }

    //Interstitial Reward ads
    @JvmField
    var mInterstitialRewardAd: RewardedInterstitialAd? = null

    @JvmStatic
    fun loadAdInterstitialReward(
        activity: Context,
        mInterstitialRewardAd: RewardedInterstitialHolderAdmob,
        adLoadCallback: AdLoadCallback
    ) {
        if (isTestDevice){
            adLoadCallback.onAdFail("is Test Device")
            return
        }
        var admobId = mInterstitialRewardAd.ads
        if (!isShowAds || !isNetworkConnected(activity)) {
            return
        }
        if (mInterstitialRewardAd.inter != null) {
            Log.d("===AdsInter", "mInterstitialRewardAd not null")
            return
        }
        val adRequest = AdRequest.Builder().build()
        mInterstitialRewardAd.isLoading = true
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_reward_id)
        }
        RewardedInterstitialAd.load(
            activity,
            admobId,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialRewardAd: RewardedInterstitialAd) {
                    mInterstitialRewardAd.inter = interstitialRewardAd
                    mInterstitialRewardAd.mutable.value = interstitialRewardAd
                    mInterstitialRewardAd.isLoading = false
                    adLoadCallback.onAdLoaded()
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialRewardAd.inter = null
                    mInterstitialRewardAd.isLoading = false
                    mInterstitialRewardAd.mutable.value = null
                    adLoadCallback.onAdFail(loadAdError.message)
                }
            })
    }

    @JvmStatic
    fun showAdInterstitialRewardWithCallback(
        activity: Activity, mInterstitialRewardAd: RewardedInterstitialHolderAdmob,
        adCallback: RewardAdCallback
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            if (AppOpenManager.getInstance().isInitialized) {
                AppOpenManager.getInstance().isAppResumeEnabled = true
            }
            adCallback.onAdFail("No internet or isShowAds = false")
            return
        }

        if (AppOpenManager.getInstance().isInitialized) {
            if (!AppOpenManager.getInstance().isAppResumeEnabled) {
                return
            } else {
                isAdShowing = false
                if (AppOpenManager.getInstance().isInitialized) {
                    AppOpenManager.getInstance().isAppResumeEnabled = false
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                if (mInterstitialRewardAd.isLoading) {
                    dialogLoading(activity)
                    delay(800)

                    mInterstitialRewardAd.mutable.observe(activity as LifecycleOwner) { reward: RewardedInterstitialAd? ->
                        reward?.let {
                            mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                            it.setOnPaidEventListener { value ->

                            }
                            mInterstitialRewardAd.inter?.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onAdClosed()
                                        Log.d("TAG", "The ad was dismissed.")
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onAdFail(adError.message)
                                        Log.d("TAG", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        isAdShowing = true
                                        adCallback.onAdShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                        Log.d("TAG", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { adCallback.onEarned() }
                        }
                    }
                } else {
                    if (mInterstitialRewardAd.inter != null) {
                        dialogLoading(activity)
                        delay(800)

                        mInterstitialRewardAd.inter?.setOnPaidEventListener {
                        }
                        mInterstitialRewardAd.inter?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onAdClosed()
                                    Log.d("TAG", "The ad was dismissed.")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onAdFail(adError.message)
                                    Log.d("TAG", "The ad failed to show.")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback.onAdShowed()
                                    Log.d("TAG", "The ad was shown.")
                                }
                            }
                        mInterstitialRewardAd.inter?.show(activity) { adCallback.onEarned() }

                    } else {
                        isAdShowing = false
                        adCallback.onAdFail("None Show")
                        dismissAdDialog()
                        if (AppOpenManager.getInstance().isInitialized) {
                            AppOpenManager.getInstance().isAppResumeEnabled = true
                        }
                        Log.d("TAG", "Ad did not load.")
                    }
                }
            }
        }
    }


    @JvmStatic
    fun loadAdReward(
        activity: Context,
        mInterstitialRewardAd: RewardHolderAdmob,
        adLoadCallback: AdLoadCallback
    ) {
        if (isTestDevice){
            adLoadCallback.onAdFail("is Test Device")
            return
        }
        var admobId = mInterstitialRewardAd.ads
        if (!isShowAds || !isNetworkConnected(activity)) {
            return
        }
        if (mInterstitialRewardAd.inter != null) {
            Log.d("===AdsInter", "mInterstitialRewardAd not null")
            return
        }
        val adRequest = AdRequest.Builder().build()
        mInterstitialRewardAd.isLoading = true
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_reward_id)
        }
        RewardedAd.load(
            activity,
            admobId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(interstitialRewardAd: RewardedAd) {
                    mInterstitialRewardAd.inter = interstitialRewardAd
                    mInterstitialRewardAd.mutable.value = interstitialRewardAd
                    mInterstitialRewardAd.isLoading = false
                    adLoadCallback.onAdLoaded()
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialRewardAd.inter = null
                    mInterstitialRewardAd.isLoading = false
                    mInterstitialRewardAd.mutable.value = null
                    adLoadCallback.onAdFail(loadAdError.message)
                }
            })
    }

    @JvmStatic
    fun showAdRewardWithCallback(
        activity: Activity, mInterstitialRewardAd: RewardHolderAdmob,
        adCallback: RewardAdCallback
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is Test Device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            if (AppOpenManager.getInstance().isInitialized) {
                AppOpenManager.getInstance().isAppResumeEnabled = true
            }
            adCallback.onAdFail("No internet or isShowAds = false")
            return
        }

        if (AppOpenManager.getInstance().isInitialized) {
            if (!AppOpenManager.getInstance().isAppResumeEnabled) {
                return
            } else {
                isAdShowing = false
                if (AppOpenManager.getInstance().isInitialized) {
                    AppOpenManager.getInstance().isAppResumeEnabled = false
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Main) {
                if (mInterstitialRewardAd.isLoading) {
                    dialogLoading(activity)
                    delay(800)

                    mInterstitialRewardAd.mutable.observe(activity as LifecycleOwner) { reward: RewardedAd? ->
                        reward?.let {
                            mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                            it.setOnPaidEventListener { value ->

                            }
                            mInterstitialRewardAd.inter?.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onAdClosed()
                                        Log.d("TAG", "The ad was dismissed.")
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AppOpenManager.getInstance().isInitialized) {
                                            AppOpenManager.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onAdFail(adError.message)
                                        Log.d("TAG", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        isAdShowing = true
                                        adCallback.onAdShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                        Log.d("TAG", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { adCallback.onEarned() }
                        }
                    }
                } else {
                    if (mInterstitialRewardAd.inter != null) {
                        dialogLoading(activity)
                        delay(800)

                        mInterstitialRewardAd.inter?.setOnPaidEventListener {
                        }
                        mInterstitialRewardAd.inter?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onAdClosed()
                                    Log.d("TAG", "The ad was dismissed.")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AppOpenManager.getInstance().isInitialized) {
                                        AppOpenManager.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onAdFail(adError.message)
                                    Log.d("TAG", "The ad failed to show.")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback.onAdShowed()
                                    Log.d("TAG", "The ad was shown.")
                                }
                            }
                        mInterstitialRewardAd.inter?.show(activity) { adCallback.onEarned() }

                    } else {
                        isAdShowing = false
                        adCallback.onAdFail("None Show")
                        dismissAdDialog()
                        if (AppOpenManager.getInstance().isInitialized) {
                            AppOpenManager.getInstance().isAppResumeEnabled = true
                        }
                        Log.d("TAG", "Ad did not load.")
                    }
                }
            }
        }
    }




    //Update New Lib
    private fun checkIdTest(activity: Activity, admobId: String?) {
//        if (admobId.equals(activity.getString(R.string.test_ads_admob_inter_id)) && !BuildConfig.DEBUG) {
//            if (dialog != null) {
//                dialog.dismiss();
//            }
//            Utils.getInstance().showDialogTitle(activity, "Warning", "Build bản release nhưng đang để id test ads", "Đã biết", DialogType.WARNING_TYPE, false, "", new DialogCallback() {
//                @Override
//                public void onClosed() {
//                }
//
//                @Override
//                public void cancel() {
//                }
//            });
//        }
    }

    private val currentTime: Long
        private get() = System.currentTimeMillis()

    fun getDeviceID(context: Context): String {
        val android_id = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return md5(android_id).uppercase(Locale.getDefault())
    }


    fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }


    fun dialogLoading(context: Activity) {
        dialogFullScreen = Dialog(context)
        dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
        dialogFullScreen?.setCancelable(false)
        dialogFullScreen?.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialogFullScreen?.window!!.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val img = dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
        img?.setAnimation(R.raw.gifloading)
        try {
            if (!context.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == false) {
                dialogFullScreen?.show()
            }
        } catch (ignored: Exception) {
        }

    }

    fun loadAndShowNativeFullScreen(
        activity: Activity,
        id: String,
        viewGroup: ViewGroup,
        layout: Int,
        mediaAspectRatio: Int,
        listener: NativeFullScreenCallBack
    ) {
        if (isTestDevice){
            listener.onLoadFailed()
            viewGroup.visibility = View.GONE
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        var adMobId: String = id
        if (isTesting) {
            adMobId = activity.getString(R.string.test_ads_admob_native_full_screen_id)
        }

        val adRequest = AdRequest.Builder().build()
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        val tagView =
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_fullscreen, null, false)
        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
        val builder = AdLoader.Builder(activity, adMobId)
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build()
        val adOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions)
            .build()
        builder.withNativeAdOptions(adOptions)
        builder.forNativeAd { nativeAd ->
            checkAdsTest(nativeAd)
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->

            }
            listener.onLoaded(nativeAd)
            populateNativeAdView(nativeAd, adView.findViewById(R.id.native_ad_view))
            try {
                viewGroup.removeAllViews()
            } catch (_: Exception) {

            }
            shimmerFrameLayout?.stopShimmer()
            try {
                viewGroup.addView(adView)
            } catch (_: Exception) {

            }

        }
        builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d("===AdmobFailed", loadAdError.toString())
                shimmerFrameLayout?.stopShimmer()
                listener.onLoadFailed()
            }
        })

        builder.build().loadAd(adRequest)

    }

    @JvmStatic
    fun loadAndGetNativeFullScreenAds(
        context: Context,
        nativeHolder: NativeHolderAdmob, mediaAspectRatio: Int,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is test device")
            return
        }
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onAdFail("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        val adRequest = AdRequest.Builder().build()
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_ads_admob_native_full_screen_id)
        }
        nativeHolder.isLoad = true
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(true).build()
        val adOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(context, nativeHolder.ads)
        adLoader.withNativeAdOptions(adOptions)
        adLoader.forNativeAd { nativeAd ->
            nativeHolder.nativeAd = nativeAd
            nativeHolder.isLoad = false
            nativeHolder.native_mutable.value = nativeAd
            checkAdsTest(nativeAd)
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let { adImpressionFacebookSDK(context, it) }
            }
            adCallback.onLoadedAndGetNativeAd(nativeAd)
        }
        adLoader.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                Log.e("Admodfail", "errorCodeAds" + adError.cause)
                nativeHolder.nativeAd = null
                nativeHolder.isLoad = false
                nativeHolder.native_mutable.value = null
                adCallback.onAdFail("errorId2_" + adError.message)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onClickAds()
            }
        })
        adLoader.build().loadAd(adRequest)
    }

    @JvmStatic
    fun loadAndGetNativeFullScreenAdsWithInter(
        context: Context,
        nativeHolder: NativeHolderAdmob, mediaAspectRatio: Int,
        adCallback: NativeAdCallbackNew
    ) {
        if (isTestDevice){
            adCallback.onAdFail("is test device")
            return
        }
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onAdFail("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        val adRequest = AdRequest.Builder().build()
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_ads_admob_native_full_screen_id)
        }
        nativeHolder.isLoad = true
        val videoOptions =
            VideoOptions.Builder().setStartMuted(true).setCustomControlsRequested(true).build()
        val adOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(context, nativeHolder.ads)
        adLoader.withNativeAdOptions(adOptions)
        adLoader.forNativeAd { nativeAd ->
            nativeHolder.nativeAd = nativeAd
            nativeHolder.isLoad = false
            nativeHolder.native_mutable.value = nativeAd
            checkAdsTest(nativeAd)
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let { adImpressionFacebookSDK(context, it) }
            }
            adCallback.onLoadedAndGetNativeAd(nativeAd)
        }
        adLoader.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                Log.e("Admodfail", "errorCodeAds" + adError.cause)
                nativeHolder.nativeAd = null
                nativeHolder.isLoad = false
                nativeHolder.native_mutable.value = null
                adCallback.onAdFail("errorId2_" + adError.message)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onClickAds()
            }
        })
        adLoader.build().loadAd(adRequest)
    }

    @JvmStatic
    fun showNativeFullScreenAdsWithLayout(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        callback: AdsNativeCallBackAdmod
    ) {
        if (isTestDevice){
            callback.NativeFailed("is test device")
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        viewGroup.removeAllViews()
        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(
                    nativeHolder.nativeAd!!,
                    adView.findViewById(R.id.native_ad_view)
                )
                shimmerFrameLayout?.stopShimmer()
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                callback.NativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.NativeFailed("None Show")
            }
        } else {
            val tagView = activity.layoutInflater.inflate(
                R.layout.layoutnative_loading_fullscreen,
                null,
                false
            )
            viewGroup.addView(tagView, 0)

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        adImpressionFacebookSDK(activity,it)
                    }

                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(
                        nativeHolder.nativeAd!!,
                        adView.findViewById(R.id.native_ad_view)
                    )
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }

                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)

                    callback.NativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.NativeFailed("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    fun loadAndShowNativeFullScreenNoShimmer(
        activity: Activity,
        id: String,
        viewGroup: ViewGroup,
        layout: Int,
        mediaAspectRatio: Int,
        listener: NativeFullScreenCallBack
    ) {
        if (isTestDevice){
            listener.onLoadFailed()
            return
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        val adRequest = AdRequest.Builder().build()
        var adMobId: String = id
        if (isTesting) {
            adMobId = activity.getString(R.string.test_ads_admob_native_full_screen_id)
        }
        val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
        val builder = AdLoader.Builder(activity, adMobId)
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build()
        val adOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions)
            .build()
        builder.withNativeAdOptions(adOptions)
        builder.forNativeAd { nativeAd ->
            listener.onLoaded(nativeAd)
            checkAdsTest(nativeAd)
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let {

                }
            }
            populateNativeAdView(nativeAd, adView.findViewById(R.id.native_ad_view))
            try {
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            } catch (_: Exception) {

            }

        }
        builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d("===AdmobFailed", loadAdError.toString())
                listener.onLoadFailed()
            }
        })

        builder.build().loadAd(adRequest)

    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAdsNoBtn(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        adCallback: NativeAdCallbackNew
    ) {
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
//        val videoOptions =
//            VideoOptions.Builder().setStartMuted(false).build()
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        val adRequest = AdRequest.Builder().build()

        var s = nativeHolder.ads
        val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        }
        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeAdLoaded()
                checkAdsTest(nativeAd)
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdViewNoBtn(nativeAd, adView, size)
                shimmerFrameLayout.stopShimmer()
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onAdPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onAdFail(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onClickAds()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adLoader.loadAd(adRequest)

        Log.e("Admod", "loadAdNativeAds")
    }

    @JvmStatic
    fun showNativeAdsWithLayoutNoBtn(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: GoogleENative,
        callback: AdsNativeCallBackAdmod
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdViewNoBtn(nativeHolder.nativeAd!!, adView, size)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }
                callback.NativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.NativeFailed("None Show")
            }
        } else {
            val tagView: View = if (size === GoogleENative.UNIFIED_MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            }
            try {
                viewGroup.addView(tagView, 0)
            } catch (_: Exception) {

            }

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        callback.onPaid(it, nativeHolder.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdViewNoBtn(nativeAd, adView, size)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }
                    callback.NativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.NativeFailed("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    fun checkAdsTest(ad: NativeAd?) {
        if (isCheckTestDevice){
            try {
                val testAdResponse = ad?.headline.toString().replace(" ", "").split(":")[0]
                Log.d("===Native", ad?.headline.toString().replace(" ", "").split(":")[0])
                val testAdResponses = arrayOf(
                    "TestAd",
                    "Anunciodeprueba",
                    "Annuncioditesto",
                    "Testanzeige",
                    "TesIklan",
                    "Anúnciodeteste",
                    "Тестовоеобъявление",
                    "পরীক্ষামূলকবিজ্ঞাপন",
                    "जाँचविज्ञापन",
                    "إعلانتجريبي",
                    "Quảngcáothửnghiệm"
                )
                isTestDevice = testAdResponses.contains(testAdResponse)
            } catch (_: Exception) {
                isTestDevice = true
                Log.d("===Native", "Error")
            }
            AppOpenManager.getInstance().setTestAds(isTestDevice)
            Log.d("===TestDevice===", "isTestDevice: $isTestDevice")
        }else{
            isTestDevice = false
        }
    }
}