import com.ysj.lib.bytecodeutil.plugin.api.IModifier
import com.ysj.lib.bytecodeutil.plugin.api.logger.YLogger
import org.objectweb.asm.tree.ClassNode
import java.util.concurrent.Executor

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("bcu-plugin")
}

bytecodeUtil {
    loggerLevel = 2
    modifiers = arrayOf(
        // 将 CustomModifier 添加到 bcu 中
        CustomModifier::class.java,
    )
    notNeed = { entryName ->
//        false
        entryName.startsWith("kotlin/")
            || entryName.startsWith("kotlinx/")
            || entryName.startsWith("javax/")
            || entryName.startsWith("org/intellij/")
            || entryName.startsWith("org/jetbrains/")
            || entryName.startsWith("org/junit/")
            || entryName.startsWith("org/hamcrest/")
            || entryName.startsWith("com/squareup/")
            || entryName.startsWith("android")
            || entryName.startsWith("com/google/android/")
            || entryName.startsWith("okhttp")
    }
}

// 演示给 CustomModifier 传递自定义参数
ext["modifier.custom"] = "这是自定义的参数"

android {
    namespace = "com.ysj.demo"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.ysj.demo"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
//    implementation("com.squareup.okhttp3:okhttp:4.9.2")

}

/**
 * 演示自定义一个 [IModifier] 实现。
 */
class CustomModifier(
    override val executor: Executor,
    override val allClassNode: Map<String, ClassNode>,
) : IModifier {

    private val logger = YLogger.getLogger(javaClass)
    override fun initialize(project: Project) {
        super.initialize(project)
        // 初始化阶段，可以通过 project 拿到所需的配置参数
        logger.lifecycle("step1：initialize")
        // 演示获取自定义参数
        logger.lifecycle(project.properties["modifier.custom"].toString())
    }

    override fun scan(classNode: ClassNode) {
        // 扫描阶段，该阶段可以获取到所有过滤后需要处理的 class
        logger.lifecycle("step2：scan -->$classNode")
        // 你可以在这里过收集需要处理的 class
        // 注意：该方法非多线程安全，内部处理记得按需加锁
    }

    override fun modify() {
        // 处理阶段，该阶段是最后一个阶段，用于修改 scan 阶段收集的 class
        logger.lifecycle("step3：modify")
    }
}