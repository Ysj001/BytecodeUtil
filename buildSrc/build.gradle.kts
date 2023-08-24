plugins {
    `kotlin-dsl`
}

private val reposDir = File("${rootDir}/../", "repos")

repositories {
    maven { url = reposDir.toURI() }
    maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public/") }
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation("com.android.tools.build:gradle-api:8.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    val groupId = "io.github.ysj001"
    val version = "1.0.8"
    val hasPlugin = groupId
        .replace(".", File.separator)
        .let { File(reposDir, it) }
        .run { isDirectory && !list().isNullOrEmpty() }
    if (hasPlugin) {
        implementation("$groupId:bytecodeutil-api:$version")
        implementation("$groupId:bytecodeutil-modifier:$version")
        implementation("$groupId:bytecodeutil-plugin:$version")
    }
}
