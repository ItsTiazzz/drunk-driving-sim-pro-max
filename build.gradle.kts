import godot.annotation.processor.classgraph.AnnotationProcessingMode
import godot.gradle.GodotLanguage
import godot.registrar.generator.RegisteredNameMode
import godot.registrar.generator.RegistrationFileIndentation
import godot.registrar.generator.RegistrationFileLayoutMode

plugins {
    id("com.utopia-rise.godot-kotlin-jvm") version "0.17.0-4.7.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

godot {
    languages = setOf(GodotLanguage.KOTLIN)

    annotationProcessingMode = AnnotationProcessingMode.Inferred

    registrationFilesLayoutMode = RegistrationFileLayoutMode.HIERARCHICAL
    registrationNameMode = RegisteredNameMode.SIMPLE_NAME
    registrationFilesIndentation = RegistrationFileIndentation.TAB

    isGodotCoroutinesEnabled = true

    javaVersion = 17
}

interface InjectedExecOps {
    @get:Inject val execOps: ExecOperations
}

tasks.register("generateAllEmbeddedJres") {
    group = "godot-kotlin-jvm"
    description = "Generate JRE's for all specified platforms."

    val platforms = listOf(
        Triple("amd64", "linux", "${System.getProperty("java.home")}/jmods"),
        Triple("amd64", "windows", "${project.property("windows_java_home")}/jmods"),
    )

    var modules: Array<String> = arrayOf(
        "java.base",
        "java.logging",
    )

    var arguments: Array<String> = arrayOf(
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages"
    )

    val outDir = { arch: String, os: String -> "srcGodot/jvm/jre-$arch-$os" }

    val injected = objects.newInstance<InjectedExecOps>()

    doLast {
        val resolvedJavaHome = System.getProperty("java.home")

        platforms.forEach { (arch, os, jmodsPath) ->
            file(outDir(arch, os)).deleteRecursively()

            injected.execOps.exec {
                commandLine(
                    "$resolvedJavaHome/bin/jlink",
                    "--module-path", jmodsPath,
                    "--add-modules", modules.joinToString(","),
                    "--output", outDir(arch, os),
                    *arguments,
                )
            }

            logger.lifecycle(
                "Custom JRE created in ${outDir(arch, os)} using modules: '${modules.joinToString(",")}', arguments: '${
                    arguments.joinToString(" ")
                }', jmods: $jmodsPath and java home: $resolvedJavaHome"
            )
        }
    }
}

tasks.register("exportAll") {
    group = "godot-kotlin-jvm"
    description = "Exports the Godot projects"

    val exportDir = "srcGodot/.export"

    val platforms = listOf(
        "Linux" to ".export/linux.zip",
        "Windows Desktop" to ".export/windows.zip"
    )

    val injected = objects.newInstance<InjectedExecOps>()

    dependsOn("build", "generateAllEmbeddedJres")

    doLast {
        file(exportDir).deleteRecursively()
        file(exportDir).mkdir()

        platforms.forEach { (platform, exportPath) ->
            injected.execOps.exec {
                commandLine(
                    project.property("godot_exec"),
                    "--headless",
                    "--path", "srcGodot",
                    "--export-release", platform, exportPath,
                )
            }
        }
    }
}

tasks.register("runGame") {
    group = "godot-kotlin-jvm"
    description = "Run the game in the debug window"

    val injected = objects.newInstance<InjectedExecOps>()

    dependsOn("build")

    doLast {
        injected.execOps.exec {
            commandLine(
                project.property("godot_exec"),
                "--path", "srcGodot"
            )
        }
    }
}
