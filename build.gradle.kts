// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        applyMavenLocal(this)
        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public/") }
        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:$ANDROID_GRADLE_VERSION")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$KOTLIN_VERSION")

        classpath("com.vanniktech:gradle-maven-publish-plugin:0.14.2")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.30")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
//        classpath("$LIB_GROUP_ID:bytecodeutil-plugin:$LIB_VERSION")
        // demo plugin
//        classpath("$PROJECT_LIB_GROUP_ID:demo-plugin:$PROJECT_LIB_VERSION")
    }
}

subprojects {
    repositories {
        applyMavenLocal(this)
        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public/") }
        maven { setUrl("https://jitpack.io") }
        google()
        mavenCentral()
    }
    projectConfigure()
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}