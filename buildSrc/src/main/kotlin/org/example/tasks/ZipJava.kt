package org.example.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.isRegularFile

abstract class ZipJava : DefaultTask() {
    @get:Input
    abstract val os: Property<String>      // e.g., "windows", "macos", "linux"

    @get:Input
    abstract val version: Property<String>

    @get:InputDirectory
    abstract val directory: DirectoryProperty

    @get:OutputFile
    abstract val archive: RegularFileProperty

    init {
        archive.set(
            os.zip(version) { platform, version -> project.layout.buildDirectory
                .file("java-$platform-$version.zip") }
                .flatMap { it })
    }

    @TaskAction
    fun zipJava() {
        val root = directory.get().asFile.toPath()
        val files = directory.get().asFileTree.files
            .map { it.toPath() }
            .filter { it.isRegularFile() }
        val archivePath = archive.get().asFile.toPath()
        Files.deleteIfExists(archivePath)
        FileSystems.newFileSystem(archivePath, mapOf("create" to true)).use { zip ->
            files.forEach { file ->
                val destination = zip.getPath(root.relativize(file).toString())
                if(destination.nameCount > 1) {
                    Files.createDirectories(destination.parent)
                }
                Files.copy(file, destination)
            }
        }

    }
}