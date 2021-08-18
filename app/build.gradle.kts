plugins {
    id("com.android.application")
    id("kotlin-android")
    id("bytecodeutil-plugin")
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")

    defaultConfig {
        minSdkVersion(29)
        applicationId = "com.ysj.lib.simpleaop"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("release") {
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

    // lint 耗时，关掉
    lintOptions {
        isCheckReleaseBuilds = false
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api("org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION")
    api("androidx.core:core-ktx:1.3.2")

    api("androidx.appcompat:appcompat:1.2.0")
    api("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    api("com.google.android.material:material:1.3.0-alpha04")
    api("androidx.legacy:legacy-support-v4:1.0.0")
    api("androidx.constraintlayout:constraintlayout:2.0.4")

    implementation("$PROJECT_LIB_GROUP_ID:bytecodeutil-api:$PROJECT_LIB_VERSION")
}