
import org.example.tasks.CreateRelease
import org.example.tasks.DownloadJavaZipTask
import org.example.tasks.UnzipJava
import org.example.tasks.ZipJava

val token: String = project.property("github.token") as String
val componentVersion = "${project.property("build.major")}.${project.property("build.minor")}.${project.property("build.number")}"
val jvmVersion: String = project.property("java.version") as String

val platforms = listOf("windows", "linux", "macos")
val zips: MutableList<Provider<RegularFile>> = mutableListOf()

platforms.forEach { platform ->

    val downloadJRE = tasks.register<DownloadJavaZipTask>("downloadJRE$platform") {
        javaVersion.set(jvmVersion)
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

tasks.register<CreateRelease>("uploadJava") {
    assets.addAll(zips)
    version.set(componentVersion)
    accessToken.set(token)
    javaVersion.set(jvmVersion)
}