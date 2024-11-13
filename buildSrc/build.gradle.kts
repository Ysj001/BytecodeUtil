plugins {
    `kotlin-dsl`
}

private val reposDir = File(rootDir, "../repos")

repositories {
    maven { url = reposDir.toURI() }
    maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
    maven { setUrl("https://maven.aliyun.com/repository/central") }
    maven { setUrl("https://maven.aliyun.com/repository/google") }
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation("com.android.tools.build:gradle-api:8.4.1")
    val properties = org.jetbrains.kotlin
        .konan.properties
        .loadProperties(File(rootDir, "../gradle.properties").absolutePath)
    val groupId = properties["bcu.groupId"] as String
    val bcuPluginVersion = properties["bcu.plugin.version"] as String
    val hasPlugin = groupId
        .replace(".", File.separator)
        .let { File(reposDir, it) }
        .run { isDirectory && !list().isNullOrEmpty() }
    if (hasPlugin) {
        implementation("$groupId:plugin:$bcuPluginVersion")
    }
}
