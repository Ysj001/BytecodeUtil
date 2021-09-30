### BytecodeUtil

本库对一些常用的字节码修改目的进行封装，目的是为了便于开发者在源码中通过注解控制编译时的字节码生成，以达到一些编码时难以实现的功能。并提供自定义字节码修改器注册接口提升你的发开效率。

#### 功能&特点

- [x] 基于 Transform + ASM，内部多线程并行处理 IO。高兼容，高效率。
- [x] 平台化，支持挂载自定义字节码修改器（Modifier）。方便自定义功能开发的同时不会有多余的 Transform 任务产生。Modifier 的执行顺序可依据开发者设置的顺序执行。
- [x] 可替代 AspectJ 的 AOP 功能
- [ ] 依赖注入功能



#### 了解&编译项目

- BytecodeUtil
  - `app` 用于演示的 Demo
  - `buildSrc`  管理 maven 发布和版本控制
  - `repos` 的本地 maven 仓库，便于开发时调试
  - `demo_plugin` 演示挂载自定义的 Modifier
  - `lib_bytecodeutil_api`  BytecodeUtil 的相关 API
  - `lib_bytecodeutil_plugin`  BytecodeUtil 的插件，用于编译时修改字节码
    - `/core/modifoer`  存放自带的字节码修改器
      - `aspect`  AOP 相关实现
      - `di` 依赖注入相关实现（待实现）

- 注意：在构建前先在项目根目录下执行该命令保持本地仓库应用最新的源码

  gradlew publishAllPublicationsToLocalRepository

- [了解 Gradle 和 Transform 点这](https://blog.csdn.net/qq_35365635/article/details/120355777)



#### 如何使用

##### 依赖

由于发布到 mavenCentral 有点麻烦，目前暂时先在 release 中下载。

1. 先在 release 中下载所需的代码包后在你的 Gradle 项目根目录下解压

2. 配置根目录下的 build.gradle 来使用解压中的 jar 包

   ```groovy
   // Top-level build file
   buildscript {
       // 解压的仓库地址
       ext.repos = uri('./repos')
       repositories {
       	// 使用仓库中的 jar
           maven { url repos }
           ... ...
       }
       dependencies {
       	... ...
           classpath "io.github.ysj001:bytecodeutil-plugin:1.0.4"
       }
   }
   
   allprojects {
       repositories {
       	// 使用仓库中的 jar
           maven { url repos }
       	... ...
       }
   }
   ```

3. application 模块的 build.gradle 配置依赖

   ```groovy
   apply plugin: 'com.android.application'
   // 使用 bytecodeutil 插件
   apply plugin: 'bytecodeutil-plugin'
   
   // 插件扩展
   bytecodeUtil {
       // 设置插件日志级别
       loggerLevel = 1
       // 挂载你所需的修改器
       modifiers = [
           // 如：挂载 AOP 修改器
           Class.forName("com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.AspectModifier")
   	]
       // 不需要处理的 jar 包内文件过滤器。合理配置可大幅提升编译速度
       notNeedJar = { entryName ->
           // 这里演示个比较通用的
           (entryName.startsWith("kotlin")
                   || entryName.startsWith("java")
                   || entryName.startsWith("org/intellij/")
                   || entryName.startsWith("org/jetbrains/")
                   || entryName.startsWith("org/junit/")
                   || entryName.startsWith("org/hamcrest/")
                   || entryName.startsWith("com/squareup/")
                   || entryName.startsWith("android")
                   || entryName.startsWith("com/google/android/"))
       }
   }
   
   ... ...
       
   dependencies {
   	... ...
       implementation "io.github.ysj001:bytecodeutil-api:1.0.4"
   }
   ```

##### 混淆配置

```text
-keepclassmembers class * {
    @com.ysj.lib.bytecodeutil.api.util.BCUKeep <methods>;
}
```

##### AOP

需要挂载该修改器：`com.ysj.lib.bytecodeutil.plugin.core.modifier.aspect.AspectModifier`

###### 介绍

Aspect Oriented Programming。面向切面编程，它的应用是指在不修改源代码的情况下给程序动态添加功能，并将这个功能统一横切到一处统一处理的一种编程方式，能充分体现了高内聚低耦合的编程思想。

###### 案例（DEMO）

- 演示在 MainActivity 的 onCreate 函数体开头插入如下打印 log 的方法

  ```kotlin
  // 使用该注解标识该类是切面类
  @Aspect
  object AopTest {
      // 使用该注解标识该方法是一个切入点方法，该方法会被织入到指定
      @Pointcut(
          target = "class:.*MainActivity",
          funName = "onCreate",
          funDesc = "\\(Landroid/os/Bundle;\\)V",
          position = POSITION_START, // 设置织入到目标方法的开头
      )
      fun log(jp: JoinPoint /* 可以拿到织入的目标方法的参数等 */) {
          Log.i(TAG, "捕获到: ${jp.target} args:${jp.args}")
      }
  }
  ```
  
- 演示代理任意使用了 IntervalTrigger 注解的方法实现间隔触发

  ```kotlin
  // 自定义一个间隔触发的注解
  @Target(AnnotationTarget.FUNCTION)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class IntervalTrigger(
      val intervalMS: Long = 1000
  )
  
  @Aspect
  object AopTest {
      var oldTriggerTime = 0L
      @Pointcut(
          target = "annotation:L.*IntervalTrigger;",
          position = POSITION_CALL, // 设置在调用位置代理源方法
      )
      fun log(callingPoint: CallingPoint) {
          val trigger = callingPoint.annotation(IntervalTrigger::class.java) ?: return
          val currentTimeMillis = System.currentTimeMillis()
          if (currentTimeMillis - oldTriggerTime < trigger.intervalMS) {
              Log.i(TAG, "log5: 禁止触发")
              return
          }
          oldTriggerTime = currentTimeMillis
          Log.i(TAG, "log5: 成功触发")
          callingPoint.call()
      }
  }
  
  // 如下：
  class MainActivity : AppCompatActivity() {
      @LogPositionReturn
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContentView(R.layout.activity_main)
          findViewById<View>(R.id.test).setOnClickListener {
              test3()
          }
      }
  
      @IntervalTrigger(500)
      private fun test3() {
          // todo something
      }
  }
  ```


