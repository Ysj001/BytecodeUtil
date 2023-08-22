import org.gradle.api.Project
import java.io.File
import java.util.Properties

/*
 * Gradle 相关常用工具。
 *
 * @author Ysj
 * Create time: 2023/2/16
 */

val File.properties: Properties
    get() {
        val properties = Properties()
        inputStream().use(properties::load)
        return properties
    }