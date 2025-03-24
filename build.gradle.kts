// Root build.gradle.kts

plugins {
    id("com.android.application") version "8.6.1" apply false // Updated AGP version
    //noinspection GradleDependency
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false // Updated Kotlin version
    id("com.google.gms.google-services") version "4.4.2" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
