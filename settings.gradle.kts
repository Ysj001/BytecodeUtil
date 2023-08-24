rootProject.name = "BytecodeUtil"

// libs
include(":lib_bytecodeutil_api")
include(":lib_bytecodeutil_plugin")
include(":lib_bytecodeutil_modifier")

private val hasPlugin = "io.github.ysj001"
    .replace(".", File.separator)
    .let { File(File(rootDir, "repos"), it) }
    .run { isDirectory && !list().isNullOrEmpty() }

if (hasPlugin) {
    // Demo
    include(":app")
    //include(":demo_plugin")
    //include(":module_test")
}
