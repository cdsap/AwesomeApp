buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.cloud:google-cloud-bigquery:2.7.0")

    }
}
plugins {
    id("org.jetbrains.kotlin.jvm") version ("1.9.10") apply false
    id("com.android.application") version "8.1.4" apply false
    id("com.android.library") version "8.1.4" apply false
}
