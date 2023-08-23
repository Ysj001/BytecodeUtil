import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType
import java.util.concurrent.TimeUnit

/*
 * 定义工程常量
 *
 * @author Ysj
 * Create time: 2023/6/30
 */

/** kotlin 依赖版本 */
const val KOTLIN_VERSION = "1.8.20"

/** com.android.tools.build:gradle version */
const val ANDROID_GRADLE_VERSION = "8.1.1"

/**
 * 工程统一配置。
 */
fun Project.projectConfigure() = afterEvaluate config@{
    val androidExt = extensions.findByType(AndroidComponentsExtension::class)
    when (androidExt) {
        is LibraryAndroidComponentsExtension -> {
            // android library
        }
        is ApplicationAndroidComponentsExtension -> {
            // android application
        }
        else -> return@config
    }
    configurations.all {
        // changing modules 如：SNAPSHOT 只缓存 40s
        resolutionStrategy.cacheChangingModulesFor(40, TimeUnit.SECONDS)
    }
    dependencies.applyKotlin()
    dependencies.applyAndroidTest()
    dependencies.applyAndroidCommon()
    dependencies.applyAndroidKtx()
}
