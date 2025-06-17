package com.lib.dktechads

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ads.detech.AdmobUtils
import com.ads.detech.utils.admod.RewardHolderAdmob
import com.ads.detech.utils.admod.RewardedInterstitialHolderAdmob
import com.ads.detech.utils.admod.callback.AdLoadCallback
import com.ads.detech.utils.admod.callback.RewardAdCallback
import com.ads.detech.utils.admod.remote.BannerPlugin
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.nativead.NativeAd
import com.lib.dktechads.databinding.ActivityMainBinding
import com.lib.dktechads.utils.AdsManagerAdmod

class MainActivity : AppCompatActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    lateinit var bannerContainer: ViewGroup
    lateinit var nativeLoader: MaxNativeAdLoader
    var rewardInterHolder = RewardedInterstitialHolderAdmob("")
    var rewardHolder = RewardHolderAdmob("")

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val nativeAds = findViewById<FrameLayout>(R.id.nativead)
        bannerContainer = findViewById<FrameLayout>(R.id.banner_container)

        com.ads.detech.ads.AdsManager.showNativePreload(this,"native_preload",binding.nativead)

        binding.btnShowNative.setOnClickListener {
        }
        binding.btnLoadInter.setOnClickListener {
            AdsManagerAdmod.loadInter(this, AdsManagerAdmod.interholder)
        }
        binding.btnLoadInterAppLovin.setOnClickListener {
        }

        binding.btnShowInterAppLovin.setOnClickListener {

        }

        binding.btnShowInter.setOnClickListener {
            AdsManagerAdmod.showInter(
                this,
                AdsManagerAdmod.interholder,
                object : AdsManagerAdmod.AdListener {
                    override fun onAdClosed() {
                        startActivity(Intent(this@MainActivity, MainActivity2::class.java))
                    }

                    override fun onFailed() {
                        startActivity(Intent(this@MainActivity, MainActivity2::class.java))
                    }
                },
                true
            )

        }

        binding.btnLoadShowInterCallback2.setOnClickListener {

        }

        binding.btnShowReward.setOnClickListener {
        }

        binding.loadNative.setOnClickListener {
            AdsManagerAdmod.loadAdsNativeNew(this, AdsManagerAdmod.nativeHolder)
        }

        binding.loadNativeMax.setOnClickListener {

        }

        binding.showNative.setOnClickListener {
            AdsManagerAdmod.showNative(this, nativeAds, AdsManagerAdmod.nativeHolder)
        }

        binding.showNativeMax.setOnClickListener {

        }
        binding.loadBanner.setOnClickListener {
            AdmobUtils.loadAndShowBannerWithConfig(this,"",5,10,binding.bannerContainer,
                BannerPlugin.BannerConfig.TYPE_ADAPTIVE,object : AdmobUtils.BannerCollapsibleAdCallback{
                override fun onClickAds() {

                }

                override fun onBannerAdLoaded(adSize: AdSize) {
                }

                override fun onAdFail(message: String) {
                }

                override fun onAdPaid(adValue: AdValue, mAdView: AdView) {
                }

            })
        }

        binding.bannerMax.setOnClickListener {

        }

        binding.btnLoadShowInterAdmob.setOnClickListener {
//            AdmobUtils.loadAndShowAdInterstitial(this, AdsManagerAdmod.interholder,object :
//                AdsInterCallBack {
//                override fun onStartAction() {
//
//                }
//
//                override fun onEventClickAdClosed() {
//                    startActivity(Intent(this@MainActivity, MainActivity2::class.java))
//                }
//
//                override fun onAdShowed() {
//
//                }
//
//                override fun onAdLoaded() {
//
//                }
//
//                override fun onAdFail(error: String?) {
//                    startActivity(Intent(this@MainActivity, MainActivity2::class.java))
//                }
//
//                override fun onClickAds() {
//
//                }
//
//                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
//
//                }
//            },true)

            com.ads.detech.ads.AdsManager.showAdsInterstitial(this,"inter", R.layout.ad_native_fullscreen){
                Toast.makeText(this, "Action", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loadAndShowNativeAdmob.setOnClickListener {
//            AdmobUtils.loadAndShowNativeAdsWithLayoutAdsCollapsible(this,
//                AdsManagerAdmod.nativeHolder,binding.nativead,R.layout.ad_template_medium,GoogleENative.UNIFIED_MEDIUM,object :
//                AdmobUtils.NativeAdCallbackNew {
//                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {
//
//                }
//
//                override fun onNativeAdLoaded() {
//
//                }
//
//                override fun onAdFail(error: String) {
//
//                }
//
//                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
//
//                }
//
//                override fun onClickAds() {
//                    val params: ViewGroup.LayoutParams = binding.nativead.layoutParams
//                    params.height = AppLovinSdkUtils.dpToPx(this@MainActivity,100)
//                    binding.nativead.layoutParams = params
//                }
//            })
            com.ads.detech.ads.AdsManager.showAdsBannerNative(this@MainActivity,"ads_banner_native",binding.nativead)
        }
        
        binding.loadReward.setOnClickListener { 
            AdmobUtils.loadAdReward(this@MainActivity,rewardHolder ,object : AdLoadCallback{
                    override fun onAdFail(message: String?) {
                        
                    }

                    override fun onAdLoaded() {
                        
                    }

                    override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                        
                    }

                }
            )
        }

        binding.showReward.setOnClickListener {
            AdmobUtils.showAdRewardWithCallback(this,rewardHolder,object :
                RewardAdCallback {
                override fun onAdClosed() {
                    Log.d("==RewardAdCallback==", "onAdClosed: ")
                }

                override fun onAdShowed() {
                    Log.d("==RewardAdCallback==", "onAdShowed: ")
                }

                override fun onAdFail(message: String?) {

                }

                override fun onEarned() {
                    Log.d("==RewardAdCallback==", "onEarned: ")
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }

            })
            
            AdmobUtils.loadAndShowRewardedInterstitialAdWithCallback(this,"",object : RewardAdCallback{
                override fun onAdClosed() {
                    
                }

                override fun onAdShowed() {
                    Handler().postDelayed({
                        AdmobUtils.dismissAdDialog()
                    },200)
                }

                override fun onAdFail(message: String?) {
                    
                }

                override fun onEarned() {
                    
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    
                }

            },true)
        }
//        ApplovinUtil.showNativeWithLayout(nativeAds,this, AdsManager.nativeHolder,R.layout.native_custom_ad_view,
//            GoogleENative.UNIFIED_MEDIUM,object :
//            NativeCallBackNew {
//            override fun onNativeAdLoaded(nativeAd: MaxAd?, nativeAdView: MaxNativeAdView?) {
//                Toast.makeText(this@MainActivity,"show success", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onAdFail(error: String) {
//                Toast.makeText(this@MainActivity,"Show failed", Toast.LENGTH_SHORT).show()
//            }
//
//                override fun onAdRevenuePaid(ad: MaxAd) {
//
//                }
//            })

        AdmobUtils.loadAndGetNativeFullScreenAds(this,AdsManagerAdmod.nativeHolderFull,MediaAspectRatio.ANY,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {
                    Log.d("==full==", "Load onNativeAdLoaded: ")
                }

                override fun onNativeAdLoaded() {
                    Log.d("==full==", "Load onNativeAdLoaded: ")
                }

                override fun onAdFail(error: String) {
                    Log.d("==full==", "Load onAdFail: ")
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
                    
                }

                override fun onClickAds() {
                    
                }

            })
    }

    override fun onResume() {
        super.onResume()
    }
}