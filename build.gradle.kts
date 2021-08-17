// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        maven(MAVEN_LOCAL)
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:$ANDROID_GRADLE_VERSION")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")

        classpath("com.vanniktech:gradle-maven-publish-plugin:0.14.2")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.30")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath("$PROJECT_LIB_GROUP_ID:bytecodeutil-plugin:$PROJECT_LIB_VERSION")
    }
}

allprojects {
    repositories {
        maven(MAVEN_LOCAL)
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}