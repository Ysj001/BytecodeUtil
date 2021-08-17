import com.android.build.gradle.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.*

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
    // 获取 module 中定义的发布信息
    val pomDesc = properties["POM_DESCRIPTION"] as String
    val pomAftId = properties["POM_ARTIFACT_ID"] as String
    val pomPkgType = properties["POM_PACKAGING"] as String
    val libGroupId = groupId ?: LIB_GROUP_ID
    val libVersion = version ?: LIB_VERSION
    mavenPublish(libGroupId, pomAftId, libVersion, pomDesc, pomPkgType) {
        maven {
            name = "local"
            url = MAVEN_LOCAL
        }
        maven {
            name = "mavenCentral"
            setUrl(libVersion.run {
                if (endsWith("SNAPSHOT")) MAVEN_CENTRAL_SNAPSHOTS
                else MAVEN_CENTRAL_RELEASE
            })
            credentials {
                username = property("mavenCentralUserName").toString()
                password = property("mavenCentralPassword").toString()
            }
        }
    }
}

/**
 * 便捷的发布到 maven 仓库
 *
 * - [gradle-developers](https://docs.gradle.org/current/userguide/publishing_maven.html)
 * - [android-developers](https://developer.android.google.cn/studio/build/maven-publish-plugin#groovy)
 *
 * @param groupId Sets the groupId for this publication.
 * @param version Sets the version for this publication.
 * @param desc The description for the publication represented by this POM.
 * @param artifactId Sets the artifactId for this publication.
 * @param packaging Sets the packaging for the publication represented by this POM.
 * @param repository 配置 maven 仓库
 */
fun Project.mavenPublish(
    groupId: String,
    artifactId: String,
    version: String,
    desc: String,
    packaging: String,
    repository: Action<RepositoryHandler>
) = afterEvaluate {
    // 添加发布需要的 plugin
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    // 判断 module 类型
    val isAndroidApp = project.plugins.hasPlugin("com.android.application")
    val isAndroidLib = project.plugins.hasPlugin("com.android.library")
    val isAndroidProject = isAndroidApp || isAndroidLib
    // 添加打包源码的任务，这样方便查看 lib 的源码
    @Suppress("UnstableApiUsage")
    if (!isAndroidProject) extensions.getByType(JavaPluginExtension::class.java).apply {
        withJavadocJar()
        withSourcesJar()
    }
    else tasks.register<Jar>("androidSourcesJar") {
        archiveClassifier.set("sources")
        from(project.extensions.getByType<LibraryExtension>().sourceSets["main"].java.srcDirs)
    }
    // 配置 maven 发布任务
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("mavenJava") {
                this.groupId = groupId
                this.artifactId = artifactId
                this.version = version
                when (packaging) {
                    "aar" -> from(components["release"])
                    "jar" -> from(components["java"])
                }
                // android 的源码打包
                if (isAndroidProject) artifact(LazyPublishArtifact(tasks.named("androidSourcesJar")))
                pom {
                    name.set(artifactId)
                    description.set(desc)
                    this.packaging = packaging
                    url.set(POM_URL)
                    licenses {
                        license {
                            name.set(POM_LICENCE_NAME)
                            url.set(POM_LICENCE_URL)
                        }
                    }
                    developers {
                        developer {
                            id.set(POM_DEVELOPER_ID)
                            name.set(POM_DEVELOPER_NAME)
                            email.set(POM_DEVELOPER_EMAIL)
                        }
                    }
                }
            }
        }
        repositories(repository)
    }
    if (JavaVersion.current().isJava9Compatible) tasks.named<Javadoc>("javadoc") {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}