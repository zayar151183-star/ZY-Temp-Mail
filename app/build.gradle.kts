plugins {
    id("com.android.application")
}

android {
    namespace = "com.zayar.tempbox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zayar.tempbox"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.3.0"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:25.4.0")
}
