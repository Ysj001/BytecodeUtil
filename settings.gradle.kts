rootProject.name = "BytecodeUtil"

// bytecode util libs
include(":lib_bcu_plugin")
include(":lib_bcu_plugin:plugin-api")

private val hasPlugin = File(rootDir, "gradle.properties")
    .inputStream()
    .use { java.util.Properties().apply { load(it) } }
    .getProperty("bcu.groupId")
    .replace(".", File.separator)
    .let { File(File(rootDir, "repos"), it) }
    .run { isDirectory && !list().isNullOrEmpty() }

if (hasPlugin) {
    // Demo
    include(":app")
}
