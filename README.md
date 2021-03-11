### ByteUtil

本库对一些常用的字节码修改目的进行封装，目的是为了便于开发者在源码中通过注解控制编译时的字节码生成，以达到一些编码时难以实现的功能。

基于 ASM + 安卓 Transform。轻量级，高性能。

目前处于开发阶段……



#### 了解&编译项目

- ByteUtil
  - app —— 用于演示的 Demo
  - repos —— ByteUtil 的本地 maven 仓库，便于开发时调试
  - lib_byteutil_api —— ByteUtil 的相关 API
  - lib_byteutil_plugin —— ByteUtil 的插件，用于编译时修改字节码
    - .../modifoer/impl/ —— 存放所有字节码修改器实现目录
      - aspect —— AOP 相关实现
      - di —— 依赖注入相关实现

注意：在构建前先在项目根目录下执行该命令保持本地仓库应用最新的源码

- ./gradlew uploadArchives



#### 实现思路&使用

##### AOP

###### 介绍

Aspect Oriented Programming。面向切面编程，它的应用是指在不修改源代码的情况下给程序动态添加功能，并将这个功能统一横切到一个类中处理的一种编程方式，能充分体现了高内聚低耦合的编程思想。

AOP 有常见的几种实现方式：拦截器；动态代理；编译期代码织入。

本库采用的是"编译期代码织入"的方式，通过一系列注解使得开发人员能在源码中定义织入行为。

###### 设计&思路

- 首先要实现 AOP 我们需要定义一个用来处理切面的类的注解 **Aspect**

  ```kotlin
  @Target(AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.BINARY)
  annotation class Aspect
  ```

  有了该注解即可在编译时查找到开发者所标注的需要进行处理的切面类了。

- 在获取到切面类后就需要定义切面所需要处理的切入点了

  因此我们需要再定义一个注解 **Pointcut**

  ```kotlin
  @Target(AnnotationTarget.FUNCTION)
  @Retention(AnnotationRetention.BINARY)
  annotation class Pointcut(
      /**
       * 目标切入点
       *
       * 不同的前缀表达不同的含义：
       * - "class:" --> 类
       * - "superClass:" --> 父类
       * - "interface:" --> 接口
       * - "annotation:" --> 注解
       */
      val target: String,
      /**
       * 切入点方法名，用于确定切入的方法
       *
       * 采用正则表达式 [Pattern.matches] 来匹配
       */
      val funName: String,
      /**
       * 切入点方法描述，用于确定切入的具体方法
       *
       * 采用正则表达式 [Pattern.matches] 来匹配
       */
      val funDesc: String,
      /**
       * 切入点的具体执行位置
       * - -1 表示插入方法末尾
       * - 0 表示插入方法开头
       * - 其它 表示插入到方法中任意位置
       */
      val position: Int,
  )
  ```

  通过该注解即可确定切入的目标类，目标方法，和目标方法中的具体位置了。

  并且该注解所标注的方法将会在该注解参数所定义的位置进行生成并调用。

- 在切入点能执行切面中的方法后我们还需要将切入点和切入方法产生联系

  因此我们需要再定义一个连接点 **JointPoint**

  ```kotlin
  class JoinPoint(
      /** 切入点的 this 获取的对象 */
      val target: Any,
      /** 切入点方法的参数 */
      val args: Array<Any?>,
  ) : Serializable
  ```

  有了连接点后，即可获得，切入对象和切入点所在方法的参数了。

  这时我们就可以将连接点作为切入方法的参数，以此使得切入方法和切入点取得联系。

###### 案例（DEMO）

- 切入 MainActivity 的类，并在其 onCreate 生命周期函数执行时打印 log

  ```kotlin
  @Aspect
  class AopTest {
      companion object {
          private const val TAG = "AopTest"
      }
      @Pointcut(
          target = "class:.*MainActivity",
          funName = "onCreate",
          funDesc = "\\(Landroid/os/Bundle;\\)V",
          position = 0,
      )
      fun log(joinPoint: JoinPoint) {
          Log.i(TAG, "捕获到: ${joinPoint.target} args:${joinPoint.args}")
      }
  }
  ```

