## BCU

本库是一个安卓平台上的基于 [ASM](https://asm.ow2.io/index.html) 实现的轻量级高性能字节码全量操作平台。目前已经兼容 `AGP 8+`。

此外，本库从设计之初就考虑了 `Transform ` 过程对构建时间的影响，因此特别优化了整体构建流程，能更好的利用缓存，增量，过滤机制，从而大幅减少构建所需的时间。

**关于构建性能可以直接看文章末尾。**

[![](https://jitpack.io/v/Ysj001/BytecodeUtil.svg)](https://jitpack.io/#Ysj001/BytecodeUtil)



### 功能&特性

- [x] **v2** 版已经支持 `AGP 7.4 ~ AGP 8+`  |  ~~（v1 老版本是基于 `AGP4` 的 `Transform` 接口）~~
- [x] 完全基于原生 `AGP` 和 `Gradle` 公开接口开发，没有 `Hook` 因此不会受到未来 `AGP` 接口变更影响。
- [x] 支持根据 `variant` 进行不同的配置和处理。
- [x] 支持 `dexBuilder` 任务的增量编译。
- [x] 支持非全量 `Transform` ，可通过 filter 控制需要进行处理的 class。
- [x] 基于 `asm-tree`  实现的接口更加易于修改字节码。
- [x] 插件平台内部多线程并行 IO 处理，提升编译速度。
- [x] 插件平台支持挂载多个自定义的字节码修改器，使用多个修改器也只会有一次 IO 产生。
- [x] 提供了 `plugin-api` 供开发者实现自定义的字节码修改器，甚至不需要再去写 `Gradle` 插件。



### **基于 BCU 实现的库**

- **[modifier-aspect](https://github.com/Ysj001/bcu-modifier-aspect) ：用于实现 AOP 的字节码修改器。**
  - **[PermissionMonitor](https://github.com/Ysj001/PermissionMonitor)：Android 隐私政策敏感权限监控。**

- **[modifier-component-di](https://github.com/Ysj001/bcu-modifier-component-di) ：用于实现组件依赖注入的字节码修改器。**

- **[StablePlugin](https://github.com/Ysj001/StablePlugin)：用于实现插件化。**



### 了解&编译项目

![BCU 架构图](readme_assets/bcu_structure.png)

- **BytecodeUtil**
  
  - `app` 用于演示
  - `buildSrc`  管理 maven 发布和版本控制项目统一配置
  - `repos` 的本地 maven 仓库，便于开发时调试
  - `lib_bcu_plugin`  插件工程编译时修改字节码在这里实现
    - `plugin-api`  插件对外提供的 API，基于此可实现自定义的字节码修改器
  
- **注意：在构建前先在项目根目录下执行该命令来生成插件后重新 sync 项目**

  `gradlew publishAllPublicationsToLocalRepository`



### 如何使用

#### Version & AGP & Gradle

[AGP 版本和 Gradle 版本关系点这里](https://developer.android.google.cn/studio/releases/gradle-plugin?hl=zh_cn#updating-gradle)

- v2 版本（**推荐使用**）
  - 基于 `AGP 8+` 的 `variant#artifacts` 接口开发
  - `min AGP 7.4` ~ `max AGP 8+` 
  - 当使用 `AGP 7.4` 时最低应使用 `Gradle 7.5` 不能使用 `Gradle 8`
  - 当使用 `AGP 8+` 时最低应使用 `Gradle 8`
- v1 版本（**已经不维护**）
  - 基于 `AGP 4`  的 `Transform` 接口开发现在不推荐使用了
  - `min AGP 4.1.3`  ~ `max AGP 7.3`



#### 使用

1. 项目已经发到 `jitpack.io` 仓库，在项目根 `build.gradle.kts` 中配置如下

   ```kotlin
   // Top-level build file
   buildscript {
       repositories {
           maven { setUrl("https://jitpack.io") }
       }
       
       dependencies {
           // BCU 插件依赖
           classpath("com.github.Ysj001.BytecodeUtil:plugin:<lastest-version>")
       }
   }
   
   subprojects {
       repositories {
           maven { setUrl("https://jitpack.io") }
       }
   }
   ```
   
2. 在 `app` 模块的 `build.gradle.kts` 中的配置如下

   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       // 添加 bcu 插件
       id("bcu-plugin")
   }
   
   // 插件扩展
   bcu {
       config { variant ->
       	// 设置插件日志级别 （0 ~ 5）
           loggerLevel = 2
       	// 挂载你所需的修改器，可以挂载多个，插件内部按顺序执行
           modifiers = arrayOf(
               // Your Modifier
           )
       }
       filterNot = { variant, entryName ->
          	// 自定义过滤规则
           // 该配置会极大影响编译性能，具体见后面性能对比
           // 返回 true 表示该 class 不进行 transform 处理
           false
       }
   }
   ```
   
3. `BCU` 会将需要保留的目标加上 `BCUKeep` 注解，因此只需如下配置即可

   ```tex
   -keepclassmembers class * {
       @com.ysj.lib.bytecodeutil.plugin.api.BCUKeep <methods>;
   }
   ```



#### 开发自定义 Modifier 并使用

**推荐直接查看 `app` 模块，或者查看基于 `BCU` 实现的库。**

开发并使用一个自定义的 `Modifier` 非常简单，主要为以下几个步骤：

1. 创建一个用于开发 `Modifier`  的 `java` 模块，并依赖 `plugin-api` 和 `gradleApi`

   ```groovy
   plugins {
       id("java-library")
       id("kotlin")
   }
   dependencies {
       implementation(gradleApi())
       implementation("com.github.Ysj001.BytecodeUtil:plugin-api:<lastest-version>")
   }
   ```

2. 继承 `IModifier` 接口并重写对应方法

   ```kotlin
   class CustomModifier(
       // 注意：executor ， allClassNode 顺序不能变
       override val executor: Executor,
       override val allClassNode: Map<String, ClassNode>,
   ) : IModifier {
   
       private val logger = YLogger.getLogger(javaClass)
       override fun initialize(project: Project, variant: com.android.build.api.variant.Variant) {
           super.initialize(project)
           // 初始化阶段，可以通过 project 拿到所需的配置参数
           logger.lifecycle("step1：initialize. variant=${variant.name}")
           // 演示获取自定义参数
           logger.lifecycle(project.properties["modifier.custom"].toString())
       }
   
       override fun scan(classNode: ClassNode) {
           // 扫描阶段，该阶段可以获取到所有过滤后需要处理的 class
           logger.lifecycle("step2：scan -->$classNode")
           // 你可以在这里收集需要处理的 class
           // 注意：该方法非多线程安全，内部处理记得按需加锁
       }
   
       override fun modify() {
           // 处理阶段，该阶段是最后一个阶段，用于修改 scan 阶段收集的 class
           logger.lifecycle("step3：modify")
       }
   }
   ```

3. 在 `app` 模块中使用 `bcu-plugin` 插件并添加这个自定义的 `Modifier` 

   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       id("bcu-plugin")
   }
   
   bcu {
       config { variant ->
           // 在这里配置你的 Modifier 实现，多个 Modifier 会按顺序依次执行
           modifiers = arrayOf(
               // 将 CustomModifier 添加到 bcu 中
               CustomModifier::class.java,
           )
       }
   }
   
   // 演示给 CustomModifier 传递自定义参数
   ext["modifier.custom"] = "这是自定义的参数"
   ```



#### 日志解析

了解 `BCU` 的日志系统有助于帮助你开发出更高性能的 `Modifier` ，它能够直观的告诉你各个不同阶段的耗时，好让你便于进行优化。

如下图所示 `BCU` 在运行过程中会执行 2 个任务。

分别为 `<variant>BCUTransformTask` 和 `<variant>BCUAppendTask` 。

`BCUAppendTask` 负责处理增量而 `BCUTransformTask`  负责处理整个修改字节码的过程，因此下面的参数解析主要针对该任务中的 log。

![log](readme_assets/log.png)

log 对应的具体含义：

| log key                         | 对应含义                                    |
| ------------------------------- | ------------------------------------------- |
| >>> loggerLevel                 | 当前 BCU 的日志等级（0~5）                  |
| >>> variant                     | 当前变体名称                                |
| >>> apply modifier              | 添加进 BCU 的 Modifier                      |
| >>> xxxModifier initialize time | xxxModifier 在 initialize 阶段的耗时        |
| >>> bcu scan time               | BCU 在 scan 阶段的总耗时                    |
| >>> xxxModifier process time    | xxxModifier 在 modify 阶段的耗时            |
| >>> bcu modify time             | BCU 在 modify 阶段的总耗时                  |
| >>> bcu transform output time   | BCU 将 transform 结果输出到 jar file 的耗时 |
| >>> total process time          | BCUTransformTask 处理过程的总耗时           |



### 关于构建性能

在聊性能前，我们需要先了解下新的 `ScopedArtifactsOperation#toTransform`  接口造成的问题。

在新 `transform`  接口中你的输出只能是一个 `jar` 文件，这会导致如下耗时问题：

1. 将 `class` 打进 `jar` 的操作只能是个单线程的 `IO` 操作，性能利用率极低。
2. `jar` 本质是个 `zip` 包，默认不配置压缩等级时在 `class` 文件打入过程还会有额外的压缩计算。
3. `dexBuilderXXX`  任务无法处理 jar 输入的增量编译，使得任意代码改动都会造成该任务的全量编译。

本库为了解决这些问题做了如下处理：

1. 设置 `jar` 的压缩等级为不压缩，缩短 `transform` 时的压缩时间和 `dexBuilderXXX` 时的解压时间。
1. 提供 `filter` 接口，减少需要进入 `transform` 的 `class` 数量，加快输出 `jar` 的速度，且减少由于 `jar` 导致的 `dexBuilderXXX` 任务缓存大面积失效问题。
1. 由于 `filter` 接口缩减了 `transform` 输出的源码，因此这部分源码需要借由 `ScopedArtifactsOperation#toAppend` 接口输出，而基于 `append` 接口的 `Task` 本身可以支持增量，并且且输出为直接的 `class` 文件目录，而这部分输出对 `dexBuilderXXX` 任务来说是也支持增量的，因此最后所有 `filter` 出去的 `class` 就都支持增量编译了。

在了解了本库对提升构建性能所做的处理后，相信你也能更加理解合理配置 `fliter` 的重要性。

此外由于 `filter` 后存在额外的计算（使用增量后 `gradle` 本身会对增量做计算），因此首次编译时构建速度会比不使用 `filter` 还要慢，但在此之后的编译就能体验到增量的超快速度了，相信这能极大提升你的代码调试效率。

**下面是在实际项目上的性能的对比测试，供你参考：**

- 硬件：`ROG GU603ZW (32G，1T NVMe)`
- gradle jvmargs：`org.gradle.jvmargs=-Xmx8g -Dfile.encoding=UTF-8`
- 代码量：![performance_base](readme_assets/performance_base.jpg)

|                             全量                             |                             增量                             |
| :----------------------------------------------------------: | :----------------------------------------------------------: |
| ![performance_1](readme_assets/performance_1.jpg)不过滤，全量 | ![performance_2](readme_assets/performance_2.jpg)不过滤，任意改动代码触发增量 |
| ![performance_3](readme_assets/performance_3.jpg)过滤后，全量 | ![performance_4](readme_assets/performance_4.jpg)过滤后，任意改动代码触发增量 |



### **其它**

- **[了解 Gradle 和 Transform 点这（文章基于 AGP4）](https://blog.csdn.net/qq_35365635/article/details/120355777)**
- **如果本项目给予了你帮助那就给个 start 吧。**
- **如果对本项目有疑问欢迎提 issues。**
- **如果想聊聊本库相关问题可加 Q 群 732198194（注明来源）**
