package com.lib.dktechads

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ads.detech.AdmobUtils
import com.ads.detech.AppOpenManager
import com.ads.detech.ApplovinUtil
import com.ads.detech.R
import com.ads.detech.callback_applovin.NativeCallBackNew
import com.ads.detech.firebase.FireBaseConfig
import com.ads.detech.utils.Utils
import com.ads.detech.utils.admod.callback.MobileAdsListener
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.lib.dktechads.databinding.ActivitySplashBinding
import com.lib.dktechads.utils.AdsManager
import com.lib.dktechads.utils.AdsManagerAdmod
import java.util.concurrent.atomic.AtomicBoolean


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    val isInitAds = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FireBaseConfig.initRemoteConfig(R.xml.remote_config_defaults,object : FireBaseConfig.CompleteListener{
            override fun onComplete() {
                FireBaseConfig.getValue("test")
                if (isInitAds.get()) {
                    return
                }
                isInitAds.set(true)
                AdmobUtils.initAdmob(this@SplashActivity, isDebug = true, isEnableAds = true,false, object : MobileAdsListener {
                    override fun onSuccess() {
                        Log.d("==initAdmob==", "initAdmob onSuccess: ")
                        AppOpenManager.getInstance()
                            .init(application, getString(R.string.test_ads_admob_app_open_new))
                        AppOpenManager.getInstance()
                            .disableAppResumeWithActivity(SplashActivity::class.java)
                        AppOpenManager.getInstance().setTestAds(false)
                        com.ads.detech.ads.AdsManager.preloadNative(this@SplashActivity,"native_preload")
                        com.ads.detech.ads.AdsManager.showAdsSplash(this@SplashActivity,"ad_config",R.layout.ad_native_fullscreen){
                            Utils.getInstance().replaceActivity(this@SplashActivity, MainActivity::class.java)
                        }
                    }
                })
            }
        })
    }
}