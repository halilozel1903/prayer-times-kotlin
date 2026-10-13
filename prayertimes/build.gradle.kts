import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.ktor.client.core)
    // Default engine. Pass any other Ktor engine (OkHttp, Android, Java) through PrayerTimesConfig.engine.
    implementation(libs.ktor.client.cio)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}

tasks.test {
    // The search and formatting tests must not depend on the machine's default locale.
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

mavenPublishing {
    publishToMavenCentral()
    // Sign only when a key is configured (Maven Central); JitPack and local builds stay unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    // JitPack serves artifacts under com.github.<user>.<repo>.
    val jitpackGroup = "com.github.halilozel1903.prayer-times-kotlin".takeIf { System.getenv("JITPACK") == "true" }
    coordinates(groupId = jitpackGroup, artifactId = "prayertimes")
    pom {
        name.set("Prayer Times Kotlin")
        description.set("Kotlin client for the free Aladhan prayer times API: Diyanet method by default, typed java.time models, next prayer countdown, Qibla bearing and Turkey's 81 provinces with Turkish-aware search.")
    }
}
