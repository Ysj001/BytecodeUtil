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
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$KOTLIN_VERSION")
    compileOnly("com.android.tools.build:gradle-api:$ANDROID_GRADLE_VERSION")
    compileOnly("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-tree:6.2")
    implementation("org.ow2.asm:asm-util:6.2")
    implementation(project(":lib_bytecodeutil_api"))
    implementation(project("api"))
}

mavenPublish(LIB_GROUP_ID, LIB_VERSION)