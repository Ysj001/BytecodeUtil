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



