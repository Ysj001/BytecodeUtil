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
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$KOTLIN_VERSION")
    compileOnly("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-tree:6.2")
    implementation("org.ow2.asm:asm-util:6.2")
}

mavenPublish()