package com.lib.dktechads

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ads.detech.AdmobUtils
import com.ads.detech.AppOpenManager
import com.ads.detech.R
import com.ads.detech.ads.AdsHolder.TAG
import com.ads.detech.cmp.GoogleMobileAdsConsentManager
import com.ads.detech.firebase.FireBaseConfig
import com.ads.detech.utils.Utils
import com.ads.detech.utils.admod.callback.MobileAdsListener
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.lib.dktechads.databinding.ActivitySplashBinding
import java.util.concurrent.atomic.AtomicBoolean


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    val isInitAds = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.ads.detech.ads.AdsManager.initNativeLayouts(listOf(
            R.layout.ad_template_medium,//native big
            R.layout.ad_template_small,//native small
            R.layout.ad_template_smallest,//native banner
            R.layout.ad_template_mediumcollapsible// native collapsible
        ))
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            Log.d(TAG, "onCreate: $token")
        })
        FireBaseConfig.initRemoteConfig(R.xml.remote_config_defaults,object : FireBaseConfig.CompleteListener{
            override fun onComplete() {
                FireBaseConfig.getValue("test")
                if (isInitAds.get()) {
                    return
                }
                isInitAds.set(true)
                val googleMobileAdsConsentManager = GoogleMobileAdsConsentManager(this@SplashActivity)
                googleMobileAdsConsentManager.gatherConsent { error ->
                    error?.let {
                        AdmobUtils.initAdmob(this@SplashActivity, isDebug = true, isEnableAds = true,false, object : MobileAdsListener {
                            override fun onSuccess() {
                                Log.d("==initAdmob==", "initAdmob onSuccess: ")
                                AppOpenManager.getInstance()
                                    .init(application, getString(R.string.test_ads_admob_app_open_new))
                                AppOpenManager.getInstance()
                                    .disableAppResumeWithActivity(SplashActivity::class.java)
                                AppOpenManager.getInstance().setTestAds(false)
                                com.ads.detech.ads.AdsManager.preloadNative(this@SplashActivity,"native_preload")
                                com.ads.detech.ads.AdsManager.showAdsSplash(this@SplashActivity,RemoteConfig.AD_CONFIG,binding.frBanner,R.layout.ad_native_fullscreen){
                                    Utils.getInstance().replaceActivity(this@SplashActivity, MainActivity::class.java)
                                }
                            }
                        })
                    }

                    if (googleMobileAdsConsentManager.canRequestAds) {
                        AdmobUtils.initAdmob(this@SplashActivity, isDebug = true, isEnableAds = true,false, object : MobileAdsListener {
                            override fun onSuccess() {

                                Log.d("==initAdmob==", "initAdmob onSuccess: ")
                                AppOpenManager.getInstance()
                                    .init(application, getString(R.string.test_ads_admob_app_open_new))
                                AppOpenManager.getInstance()
                                    .disableAppResumeWithActivity(SplashActivity::class.java)
                                AppOpenManager.getInstance().setTestAds(false)
                                com.ads.detech.ads.AdsManager.preloadNative(this@SplashActivity,"native_preload")
                                com.ads.detech.ads.AdsManager.showAdsSplash(this@SplashActivity,RemoteConfig.AD_CONFIG,binding.frBanner,R.layout.ad_native_fullscreen){
                                    Utils.getInstance().replaceActivity(this@SplashActivity, MainActivity::class.java)
                                }
                            }
                        })
                    }
                }

            }
        })
    }
}