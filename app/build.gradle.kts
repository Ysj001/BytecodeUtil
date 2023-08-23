plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("bytecodeutil-plugin")
}

bytecodeUtil {
    loggerLevel = 0
    modifiers = arrayOf(
        Class.forName("com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.AspectModifier")
        // 演示挂载 demo_plugin 插件中的修改器
//        Class.forName("com.ysj.demo.plugin.TestModifier")
    )
    notNeedJar = { entryName ->
        entryName.startsWith("kotlin/")
            || entryName.startsWith("kotlinx/")
            || entryName.startsWith("javax/")
            || entryName.startsWith("org/intellij/")
            || entryName.startsWith("org/jetbrains/")
            || entryName.startsWith("org/junit/")
            || entryName.startsWith("org/hamcrest/")
            || entryName.startsWith("com/squareup/")
//                || entryName.startsWith("androidx/")
//                || entryName.startsWith("android/")
//                || entryName.startsWith("com/google/android/")
    }
}

android {
    namespace = "com.ysj.demo"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.ysj.demo"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":lib_bytecodeutil_api"))

//    implementation(project(":module_test"))
//    implementation("io.github.ysj00:module_test:1.0.8")
}