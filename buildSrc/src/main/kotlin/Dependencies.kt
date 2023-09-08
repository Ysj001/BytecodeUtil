import org.gradle.api.artifacts.dsl.DependencyHandler

/*
 * 依赖配置
 *
 * @author Ysj
 * Create time: 2023/2/21
 */


fun DependencyHandler.applyKotlin(configName: String = "implementation") {
//    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.8.0')
    add(configName, "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    add(configName, "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
}

fun DependencyHandler.applyAndroidTest() {
    add("testImplementation", "junit:junit:4.13.2")
    add("androidTestImplementation", "androidx.test.ext:junit:1.1.5")
    add("androidTestImplementation", "androidx.test.ext:junit-ktx:1.1.5")
    add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.5.1")
}

fun DependencyHandler.applyAndroidCommon(configName: String = "implementation") {
    add(configName, "androidx.appcompat:appcompat:1.6.1")
    add(configName, "com.google.android.material:material:1.9.0")
    add(configName, "androidx.constraintlayout:constraintlayout:2.1.4")
    add(configName, "androidx.recyclerview:recyclerview:1.3.0")
    add(configName, "androidx.viewpager2:viewpager2:1.0.0")
}

fun DependencyHandler.applyAndroidKtx(configName: String = "implementation") {
    add(configName, "androidx.core:core-ktx:1.9.0")
    add(configName, "androidx.activity:activity-ktx:1.6.1")
    add(configName, "androidx.fragment:fragment-ktx:1.5.5")
    val lifecycleVersion = "2.6.1"
    add(configName, "androidx.lifecycle:lifecycle-livedata-core-ktx:$lifecycleVersion")
    add(configName, "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    add(configName, "androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycleVersion")
    add(configName, "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    add(configName, "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
}
