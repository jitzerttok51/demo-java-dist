
import org.example.tasks.DownloadJavaZipTask
import org.example.tasks.UnzipJava
import org.example.tasks.ZipJava

val componentVersion = "1.0.1"

val platforms = listOf("windows", "linux", "macos")
val zips: MutableList<Provider<RegularFile>> = mutableListOf()

platforms.forEach { platform ->

    val downloadJRE = tasks.register<DownloadJavaZipTask>("downloadJRE$platform") {
        javaVersion.set("17")
        os.set(platform)
    }

    val unzipJRE = tasks.register<UnzipJava>("unzipJRE$platform") {
        os.set(platform)
        archive.set(downloadJRE.flatMap { it.outputFile })
    }

    val zipJRE = tasks.register<ZipJava>("zipJRE$platform") {
        os.set(platform)
        version.set(componentVersion)
        directory.set(unzipJRE.flatMap { it.output })
    }

    zips.add(zipJRE.flatMap { it.archive })
}

tasks.register("distJava") {
    inputs.files(zips)
}