import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

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

// Generate Javadoc documentation
tasks.register<Javadoc>("generateJavadoc") {
    dependsOn("compileDebugJavaWithJavac")

    val javaCompileTask = tasks.named<JavaCompile>("compileDebugJavaWithJavac")

    // Include all Java source files
    source(fileTree("src/main/java") {
        include("**/*.java")
    })

    // Use the full classpath resolved by the Java compilation task (includes transformed AAR jars and Android SDK)
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