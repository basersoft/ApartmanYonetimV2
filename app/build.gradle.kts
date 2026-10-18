plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.baser.apartman"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.baser.apartman"
        minSdk = 21
        targetSdk = 35
        versionCode = 2
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")  // Tek sürüm
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Security
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Network - SADECE BİR TANE KULLANIN (Volley veya Retrofit, ikisi birden gerekmez)
    // Tercih 1: Volley (daha basit)
    implementation("com.android.volley:volley:1.2.1")  // Tek Volley

    // Tercih 2: Retrofit + OkHttp (daha modern) - EĞER BUNU KULLANIYORSANIZ Volley'İ KALDIRIN
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")  // Tek OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Location
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Lifecycle & Coroutines - Sadece tek sürüm kullanın
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")  // Tek sürüm
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // UI Components
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
