package org.example.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import kotlin.io.path.isRegularFile

abstract class UnzipJava : DefaultTask() {
    @get:Input
    abstract val os: Property<String>      // e.g., "windows", "macos", "linux"

    @get:InputFile
    abstract val archive: RegularFileProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    init {
        output.set(os.flatMap { project.layout.buildDirectory.dir("java-$it") })
    }

    @TaskAction
    fun unzipJava() {
        val tmp = os.flatMap { project.layout.buildDirectory.dir("tmp/java-$it") }
        project.copy {
            from(project.zipTree(archive))
            into(tmp)
        }

        val root = tmp.get().asFile.toPath()
        val newRoot = output.get().asFile.toPath()

        project.delete(output)
        tmp.get().asFileTree.files
            .map { it.toPath() }
            .filter { it.isRegularFile() }
            .forEach { file ->
            val relativeToRoot = root.relativize(file)
            val newPath = newRoot.resolve(relativeToRoot.subpath(1, relativeToRoot.nameCount))
            Files.createDirectories(newPath.parent)
            Files.deleteIfExists(newPath)
            Files.copy(file, newPath)
        }

        project.delete(tmp)

    }
}