plugins {
    id("groovy")
    id("java-library")
    id("kotlin")
}

group = LIB_GROUP_ID
version = LIB_VERSION

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(project("plugin-api"))
    compileOnly("com.android.tools.build:gradle-api:$ANDROID_GRADLE_VERSION")
}

mavenPublish()