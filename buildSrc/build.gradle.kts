import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public/") }
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation("com.android.tools.build:gradle-api:8.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
}
