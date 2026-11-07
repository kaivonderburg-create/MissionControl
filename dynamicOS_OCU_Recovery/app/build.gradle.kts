plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "dynamic.os.ocu"; compileSdk = 35
    defaultConfig { applicationId = "dynamic.os.ocu"; minSdk = 29; targetSdk = 35; versionCode = 5; versionName = "0.5.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
