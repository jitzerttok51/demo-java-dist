package org.example.tasks

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.json.JSONArray
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

val client = OkHttpClient()

fun getZuluJRE(version: String, os: String): Pair<String, String> {

        val baseUrl: String = "https://api.azul.com/metadata/v1/zulu/packages/"

        // Use HttpUrl.Builder to safely add all query parameters
        val url: HttpUrl? = baseUrl.toHttpUrlOrNull()?.newBuilder()
             ?.addQueryParameter("java_version", version)
             ?.addQueryParameter("os", os)
             ?.addQueryParameter("arch", "x64")
             ?.addQueryParameter("archive_type", "zip")
             ?.addQueryParameter("java_package_type", "jre")
             ?.addQueryParameter("javafx_bundled", "false")
             ?.addQueryParameter("crac_supported", "false")
             ?.addQueryParameter("crs_supported", "false")
             ?.addQueryParameter("latest", "true")
             ?.build()

        if(url == null) {
            throw IOException("Error: Base URL is invalid or could not be parsed: $baseUrl")
        }

        println("Constructed URL: $url")

        val request = Request.Builder()
            .url(url)
            .get() // Explicitly set GET method, though it's default
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    println("Request successful! HTTP ${response.code}")
                    println("Response body (first 500 chars):\n${bodyString}")
                    // You would typically parse this JSON response here if needed

                    if(bodyString == null) {
                        throw IOException("Empty body")
                    }

                    val body = JSONArray(bodyString)
                    if(body.isEmpty()) {
                        throw IOException("Now java with $version for $os")
                    }

                    return Pair(
                        body.getJSONObject(0).getString("name"),
                        body.getJSONObject(0).getString("download_url")
                    )
                } else {
                    System.err.println("Request failed! HTTP ${response.code} - ${response.message}")
                    response.body?.string()?.let { System.err.println("Error Body: $it") }
                    throw IOException("Request failed! HTTP ${response.code} - ${response.message}")
                }
            }
        } catch (e: IOException) {
            System.err.println("Network error when accessing Azul API: ${e.message}")
            throw e;
        } catch (e: Exception) {
            System.err.println("An unexpected error occurred: ${e.message}")
            throw e;
        }
}

abstract class DownloadJavaZipTask : DefaultTask() {
    @get:Input
    abstract val javaVersion: Property<String> // e.g., 17

    @get:Input
    abstract val os: Property<String>      // e.g., "windows", "macos", "linux"

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "download"
        description = "Downloads a specific Java Development Environment (JRE) from Azul CDN."

        outputFile.set(
            javaVersion
                .zip(os) { version, platform -> getZuluJRE(version, platform).first }
                .flatMap { project.layout.buildDirectory.file(it) }
        )
    }

    @TaskAction
    fun downloadJavaZip() {
        val url = getZuluJRE(javaVersion.get(), os.get()).second
        val conn = (URI.create(url).toURL().openConnection() as HttpURLConnection)
        conn.connect()
        conn.inputStream.use { input ->
            FileOutputStream(outputFile.asFile.get()).use { output ->
                input.copyTo(output)
            }
        }
    }
}