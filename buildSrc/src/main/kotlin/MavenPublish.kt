import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType

/*
 * Maven 发布相关扩展
 *
 * @author Ysj
 * Create time: 2021/6/29
 */

/**
 * 便捷的发布到 maven 仓库
 *
 * 需要在该 module 根目录的的 gradle.properties 中定义：
 * - POM_DESCRIPTION
 * - POM_ARTIFACT_ID
 * - POM_PACKAGING
 */
fun Project.mavenPublish(groupId: String? = null, version: String? = null) {
    // 添加发布需要的 plugin
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    // 获取 module 中定义的发布信息
    val pomGroupId = groupId ?: this.group as String
    val pomAftId = properties["POM_ARTIFACT_ID"] as String
    val pomVersion = version ?: this.version as String
    val pomDesc = properties["POM_DESCRIPTION"] as String
    val pomUrl = properties["POM_URL"] as String
    val pomPkgType = properties["POM_PACKAGING"] as String
    // 添加打包源码的任务，这样方便查看 lib 的源码
    if (pomPkgType == "aar") {
        extensions.getByType(LibraryAndroidComponentsExtension::class).finalizeDsl {
            it.publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }
    } else {
        extensions.getByType<JavaPluginExtension>().apply {
            withJavadocJar()
            withSourcesJar()
        }
    }
    mavenPublish(pomGroupId, pomAftId, pomVersion, pomDesc, pomUrl, pomPkgType) {
        maven {
            name = "local"
            url = MAVEN_LOCAL
        }
    }
}

/**
 * 便捷的发布到 maven 仓库
 *
 * - [gradle-developers](https://docs.gradle.org/current/userguide/publishing_maven.html)
 * - [android-developers](https://developer.android.google.cn/studio/build/maven-publish-plugin#groovy)
 *
 * @param pomGroupId Sets the groupId for this publication.
 * @param pomArtifactId Sets the artifactId for this publication.
 * @param pomVersion Sets the version for this publication.
 * @param pomDesc The description for the publication represented by this POM.
 * @param pomUrl The URL for the publication represented by this POM.
 * @param packaging Sets the packaging for the publication represented by this POM.
 * @param repository 配置 maven 仓库
 */
fun Project.mavenPublish(
    pomGroupId: String,
    pomArtifactId: String,
    pomVersion: String,
    pomDesc: String,
    pomUrl: String,
    packaging: String,
    repository: Action<RepositoryHandler>
) = afterEvaluate {
    // 配置 maven 发布任务
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("mavenJava") {
                this.groupId = pomGroupId
                this.artifactId = pomArtifactId
                this.version = pomVersion
                when (packaging) {
                    "aar" -> from(components["release"])
                    "jar" -> from(components["java"])
                }
                pom {
                    this.name.set(pomArtifactId)
                    this.description.set(pomDesc)
                    this.packaging = packaging
                    this.url.set(pomUrl)
                    developers {
                        developer {
                            this.id.set(POM_DEVELOPER_ID)
                            this.name.set(POM_DEVELOPER_NAME)
                        }
                    }
                }
            }
        }
        repositories(repository)
    }
}