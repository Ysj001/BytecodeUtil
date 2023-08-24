rootProject.name = "BytecodeUtil"

// bytecode util libs
include(":lib_bcu_plugin")
include(":lib_bcu_plugin:plugin-api")
// aspect modifier
include(":lib_modifier_aspect")
include(":lib_modifier_aspect:aspect-api")

private val hasPlugin = "io.github.ysj001.bcu"
    .replace(".", File.separator)
    .let { File(File(rootDir, "repos"), it) }
    .run { isDirectory && !list().isNullOrEmpty() }

if (hasPlugin) {
    // Demo
    include(":app")
}
