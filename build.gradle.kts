plugins {
    // AGP 9.1(material3 1.5 要求)。KSP 暂不支持内置 Kotlin,
    // 故经 gradle.properties(android.builtInKotlin=false)保留 kotlin-android 路径。
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.5" apply false
}
