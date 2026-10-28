import java.util.Properties
import java.util.UUID

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val metricKey = providers.environmentVariable("METRIC_KEY")
    .orElse(localProperties.getProperty("METRIC_KEY").orEmpty()).get().trim()
val supportLink = localProperties.getProperty("SUPPORT_LINK")
    ?.takeIf { it.isNotBlank() }
    ?: error("Set SUPPORT_LINK in local.properties")
val contactProperties = listOf(
    "CONTACT_TELEGRAM_USERNAME",
    "CONTACT_TELEGRAM_CHANNEL",
    "CONTACT_BOOSTY_LINK",
    "CONTACT_EMAIL"
).associateWith { name ->
    localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: error("Set $name in local.properties")
}

android {
    namespace = "ru.missclick.wolfwisdom"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.missclick.wolfwisdom"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            "String",
            "SUPPORT_LINK",
            "\"${supportLink.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
        contactProperties.forEach { (name, value) ->
            buildConfigField("String", name, "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            versionNameSuffix = "-debug"
            optimization { enable = false }
            buildConfigField("boolean", "LOGGING_ENABLED", "true")
        }
        release {
            isDebuggable = false
            isJniDebuggable = false
            optimization { enable = true }
            buildConfigField("boolean", "LOGGING_ENABLED", "false")
            buildConfigField("String", "METRIC_KEY", "\"${metricKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")
}

// Debug builds do not need production telemetry credentials.
val validateReleaseConfiguration = tasks.register("validateReleaseConfiguration") {
    group = "verification"
    description = "Checks the AppMetrica key before building release artifacts."
    val configuredMetricKey = metricKey
    doLast {
        require(runCatching { UUID.fromString(configuredMetricKey) }.isSuccess) {
            "Set a valid METRIC_KEY in local.properties or the METRIC_KEY environment variable"
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseConfiguration)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    releaseImplementation(libs.appmetrica.analytics)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
