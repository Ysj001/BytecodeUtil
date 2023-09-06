rootProject.name = "BytecodeUtil"

// bytecode util libs
include(":lib_bcu_plugin")
include(":lib_bcu_plugin:plugin-api")
// aspect modifier
include(":lib_modifier_aspect")
include(":lib_modifier_aspect:aspect-api")
// component di modifier
include(":lib_modifier_component_di")
include(":lib_modifier_component_di:component-di-api")

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
    include(":demo1")
    include(":demo1:demo1-api")
}
