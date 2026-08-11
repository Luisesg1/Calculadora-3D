import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Supabase credentials are read from local.properties (kept out of source control).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val supabaseUrl: String = localProps.getProperty("SUPABASE_URL", "")
val supabaseAnonKey: String = localProps.getProperty("SUPABASE_ANON_KEY", "")
// OAuth Web client id (from Google Cloud Console) — used by Google Sign-In via Credential Manager.
val googleWebClientId: String = localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")

// Release signing — read from keystore.properties (kept out of source control).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasSigning = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "com.print3d.calculator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.print3d.calculator"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        // AdMob: default to Google's TEST ids so debug builds never touch live ads.
        // Release overrides with the real ids below.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "ADMOB_INTERSTITIAL", "\"ca-app-pub-3940256099942544/1033173712\"")
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Real AdMob ids — only shipped in release, so test clicks can't hit them.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-4175062533825097~7704459108"
            buildConfigField("String", "ADMOB_INTERSTITIAL", "\"ca-app-pub-4175062533825097/8985751184\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// supabase-auth pulls androidx.browser 1.10.0 (OAuth Custom Tabs) which needs AGP 8.9.1+.
// We only use email/password auth, so pin browser to a version compatible with AGP 8.7.3.
configurations.all {
    resolutionStrategy { force("androidx.browser:browser:1.8.0") }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)

    implementation(libs.play.services.ads)
    implementation(libs.billing.ktx)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
