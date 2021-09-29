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

##### 混淆配置

```text
-keepclassmembers class * {
    @com.ysj.lib.bytecodeutil.api.util.BCUKeep <methods>;
}
```

##### AOP

###### 介绍

Aspect Oriented Programming。面向切面编程，它的应用是指在不修改源代码的情况下给程序动态添加功能，并将这个功能统一横切到一处统一处理的一种编程方式，能充分体现了高内聚低耦合的编程思想。

###### 案例（DEMO）

- 在 MainActivity 的 onCreate 函数体开头插入如下打印 log 的方法

  ```kotlin
  @Aspect
  object AopTest {
      @Pointcut(
          target = "class:.*MainActivity",
          funName = "onCreate",
          funDesc = "\\(Landroid/os/Bundle;\\)V",
          position = POSITION_START,
      )
      fun log(joinPoint: JoinPoint) {
          Log.i(TAG, "捕获到: ${joinPoint.target} args:${joinPoint.args}")
      }
  }
  ```
  
- 代理任意方法实现间隔触发

  ```kotlin
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
          position = POSITION_CALL,
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
  ```

- 

