plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.cp470.groupproject"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cp470.groupproject"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // MVVM
        implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
        implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

    // Unit Testing
        testImplementation("org.robolectric:robolectric:4.13")
        testImplementation("androidx.test:core:1.6.1")
        testImplementation("androidx.test.ext:junit:1.2.1")
        testImplementation("androidx.arch.core:core-testing:2.2.0")
    }