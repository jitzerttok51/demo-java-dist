plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // Or your desired version
    implementation("com.squareup.okio:okio:3.9.0")      // Okio is a dependency of OkHttp
    implementation("org.json:json:20240303") // Or your desired version
}

