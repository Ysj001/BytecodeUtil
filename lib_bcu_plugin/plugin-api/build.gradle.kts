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
    compileOnly("com.android.tools.build:gradle-api:$ANDROID_GRADLE_VERSION")
    api("org.ow2.asm:asm:9.2")
    api("org.ow2.asm:asm-tree:9.2")
    api("org.ow2.asm:asm-util:9.2")
}

mavenPublish()