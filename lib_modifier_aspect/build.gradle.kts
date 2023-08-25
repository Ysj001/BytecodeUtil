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
    implementation(project("aspect-api"))
    implementation(project(":lib_bcu_plugin:plugin-api"))
}

mavenPublish()