
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import java.util.Properties

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

        // Read USDA API key from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")

        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }

        val usdaApiKey = providers.gradleProperty("USDA_API_KEY")
            .orElse(localProperties.getProperty("USDA_API_KEY", ""))
            .get()

        // Make the USDA API key available through BuildConfig
        buildConfigField(
            "String",
            "USDA_API_KEY",
            "\"${usdaApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    // Generates BuildConfig.java so code can read build-specific values like AI_BASE_URL
    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // The exported schemas (app/schemas) are read by migration tests as assets.
    // Unit tests with Robolectric use the assets of the debug build.
    sourceSets {
        getByName("debug").assets.directories.add("schemas")
    }

    buildTypes {
        debug {
            // Emulator → laptop through "adb reverse tcp:3000 tcp:3000" (run once after each emulator start)
            buildConfigField("String", "AI_BASE_URL", "\"http://localhost:3000/api/v1/\"")
        }
        release {
            optimization {
                enable = false
            }
            // NEW: no deployed backend yet. Replace with the real HTTPS URL if it is ever hosted.
            buildConfigField("String", "AI_BASE_URL", "\"https://example.invalid/api/v1/\"")
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
    implementation("androidx.recyclerview:recyclerview:1.4.0")
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

    // WorkManager: background tasks (Java version, not -ktx)
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Unit Testing
    testImplementation("org.mockito:mockito-core:5.19.0")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // MigrationTestHelper: checks that a migration produces the schema Room expects
    testImplementation("androidx.room:room-testing:2.6.1")

    // Networking: AI assistant & recipes (Team Member 5) and the USDA scanner (Team Member 3) share Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Loads recipe photos from URLs with caching (Team Member 5)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // CameraX - Camera preview and barcode scanning (Team Member 3)
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // Google ML Kit - Barcode recognition (Team Member 3)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}

// Generate Javadoc documentation
tasks.register<Javadoc>("generateJavadoc") {
    dependsOn("compileDebugJavaWithJavac")

    val javaCompileTask = tasks.named<JavaCompile>("compileDebugJavaWithJavac")

    // Include all Java source files
    source(fileTree("src/main/java") {
        include("**/*.java")
    })

    // Use the full classpath resolved by the Java compilation task
    classpath = javaCompileTask.get().classpath

    // Configure Javadoc output
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }

    // Output directory
    destinationDir = file("${projectDir}/doc/javadoc")

    // Do not fail because of missing documentation comments
    isFailOnError = false
}
