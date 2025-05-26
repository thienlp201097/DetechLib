import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.ads.detech"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

tasks.register("generateRemoteConfig") {
    group = "codegen"
    description = "Sinh file RemoteConfig.kt từ remote_config_defaults.xml"

    doLast {
        val xmlFile = file("${rootDir}/app/src/main/res/xml/remote_config_defaults.xml")
        val outputFile = file("src/main/java/com/ads/detech/config/RemoteConfig.kt")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val entries = doc.getElementsByTagName("entry")

        val builder = StringBuilder()
        builder.appendLine("package com.ads.detech.config")
        builder.appendLine()
        builder.appendLine("object RemoteConfig {")

        fun toScreamingSnakeCase(input: String): String {
            return input.replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .replace(Regex("[^A-Za-z0-9]"), "_")
                .uppercase()
        }

        for (i in 0 until entries.length) {
            val entry = entries.item(i)
            val key = entry.childNodes.item(1).textContent.trim()
            val value = entry.childNodes.item(3).textContent.trim()
            val constKey = toScreamingSnakeCase(key)
            builder.appendLine("    const val $constKey = \"$value\"")
        }

        builder.appendLine("}")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(builder.toString())

        println("✅ RemoteConfig.kt đã được tạo tại: ${outputFile.absolutePath}")
    }
}

// Tự động chạy trước build
tasks.named("preBuild") {
    dependsOn("generateRemoteConfig")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")

    // UI
    implementation("androidx.appcompat:appcompat:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    implementation("com.pnikosis:materialish-progress:1.7")
    implementation("com.facebook.shimmer:shimmer:0.5.0@aar")

    // Ads
    implementation("com.applovin:applovin-sdk:13.2.0")
    implementation("com.google.android.gms:play-services-ads:24.2.0")
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    // Other
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.airbnb.android:lottie:6.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")

    //Adjust
    implementation("com.adjust.sdk:adjust-android:5.2.0")
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-config")
}

abstract class GenerateRemoteConfigTask : DefaultTask() {

    init {
        group = "codegen"
        description = "Tự động sinh file RemoteConfig.kt từ remote_config_defaults.xml"
    }

    @TaskAction
    fun generate() {
        val xmlFile = file("src/main/res/xml/remote_config_defaults.xml")
        val outputFile = file("src/main/java/com/example/config/RemoteConfig.kt")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val entries = doc.getElementsByTagName("entry")

        val builder = StringBuilder()
        builder.appendLine("package com.example.config")
        builder.appendLine()
        builder.appendLine("object RemoteConfig {")

        fun toScreamingSnakeCase(input: String): String {
            return input.replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .replace(Regex("[^A-Za-z0-9]"), "_")
                .uppercase()
        }

        for (i in 0 until entries.length) {
            val entry = entries.item(i)
            val key = entry.childNodes.item(1).textContent.trim()
            val value = entry.childNodes.item(3).textContent.trim()
            val constKey = toScreamingSnakeCase(key)
            builder.appendLine("    const val $constKey = \"$value\"")
        }

        builder.appendLine("}")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(builder.toString())

        println("✅ Đã tạo file RemoteConfig.kt tại: ${outputFile.absolutePath}")
    }
}