import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation("com.android.tools.build:gradle:4.1.3")
    implementation("com.squareup.okhttp3:okhttp:4.3.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.languageVersion = "1.4.10"
}