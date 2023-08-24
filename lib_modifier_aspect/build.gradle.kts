plugins {
    id("java-library")
    id("kotlin")
}

group = LIB_GROUP_ID
version = LIB_VERSION

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$KOTLIN_VERSION")
    implementation(project("api"))
}

mavenPublish(LIB_GROUP_ID, LIB_VERSION)