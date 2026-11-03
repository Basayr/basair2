import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val quranAsset = layout.projectDirectory.file("src/main/assets/quran-uthmani.txt")
val quranSha256 = "ac0724796cbbda0f4801470fbbd11d0f3c5802067bae0493466d0128b0c667af"

tasks.register("downloadVerifiedQuranText") {
    outputs.file(quranAsset)
    outputs.upToDateWhen { false }
    doLast {
        val target = quranAsset.asFile
        fun sha256(file: File): String =
            MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes())
                .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

        if (!target.exists() || sha256(target) != quranSha256) {
            target.parentFile.mkdirs()
            val temporary = File(target.parentFile, target.name + ".download")
            val source = URL(
                "https://raw.githubusercontent.com/iptvchecker/content-tanzil/main/tanzil-uthmani-1.1.txt"
            )
            val connection = source.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Basair2-Android-Build")
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.inputStream.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            check(temporary.length() == 1334737L) {
                "Unexpected Tanzil text size; refusing to package Quran data."
            }
            check(sha256(temporary) == quranSha256) {
                "Tanzil text checksum mismatch; refusing to package Quran data."
            }
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) { "Could not install verified Quran asset." }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("downloadVerifiedQuranText")
}

android {
    namespace = "com.basair.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.basair.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 5
        versionName = "0.3.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
