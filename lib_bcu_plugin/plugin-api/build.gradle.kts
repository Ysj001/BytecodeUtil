plugins {
    id("groovy")
    id("java-library")
    id("kotlin")
}

group = properties["bcu.groupId"] as String
version = properties["bcu.plugin.version"] as String

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    api(gradleApi())
    api(localGroovy())
    api("org.ow2.asm:asm:9.2")
    api("org.ow2.asm:asm-tree:9.2")
    compileOnly("com.android.tools.build:gradle-api:$ANDROID_GRADLE_VERSION")
}

mavenPublish()