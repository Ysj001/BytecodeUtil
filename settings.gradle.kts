rootProject.name = "BytecodeUtil"

// libs
include(":lib_bytecodeutil_api")
include(":lib_bcu_plugin")
include(":lib_bcu_plugin:api")

private val hasPlugin = "io.github.ysj001.bcu"
    .replace(".", File.separator)
    .let { File(File(rootDir, "repos"), it) }
    .run { isDirectory && !list().isNullOrEmpty() }

if (hasPlugin) {
    // Demo
    include(":app")
    //include(":demo_plugin")
    //include(":module_test")
}
