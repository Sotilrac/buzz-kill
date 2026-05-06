import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// AGP 9 ships built-in Kotlin support, so kotlin-android is no longer applied
// explicitly — adding it would clash with the AGP-managed `kotlin` extension.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ca.asmat.buzzkill"
    compileSdk = 36

    defaultConfig {
        applicationId = "ca.asmat.buzzkill"
        minSdk = 26
        targetSdk = 36
        // Plain literals so F-Droid's checkupdates regex can parse them.
        // Bump both when cutting a tag; CI asserts the tag matches the value below.
        versionCode = 10007
        versionName = "1.0.7"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets["main"].java.srcDirs("src/main/kotlin")
    sourceSets["test"].java.srcDirs("src/test/kotlin")

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

// Regenerates launcher icon resources from tools/icon-1024.png. Wired into
// preBuild so a fresh source PNG triggers a regen automatically; skipped when
// inputs+outputs are unchanged. Requires `python3` + Pillow on the host.
val generateIcons by tasks.registering(Exec::class) {
    description = "Regenerate launcher icon mipmaps from tools/icon-1024.png"
    group = "buzzkill"
    workingDir = rootDir
    commandLine("python3", "$rootDir/tools/generate-icons.py")

    inputs.file("$rootDir/tools/icon-1024.png").withPropertyName("source")
    inputs.file("$rootDir/tools/generate-icons.py").withPropertyName("script")
    outputs.dirs(
        "$projectDir/src/main/res/mipmap-mdpi",
        "$projectDir/src/main/res/mipmap-hdpi",
        "$projectDir/src/main/res/mipmap-xhdpi",
        "$projectDir/src/main/res/mipmap-xxhdpi",
        "$projectDir/src/main/res/mipmap-xxxhdpi",
    )
    outputs.file("$projectDir/src/main/res/drawable/ic_launcher_foreground.webp")
    outputs.cacheIf { true }

    // Don't fail the build if Python/Pillow is missing (e.g. on CI runners
    // without it) — the generated webps are checked into git and will be used
    // as-is. A real source change requires a host with Pillow installed.
    isIgnoreExitValue = true
    doFirst {
        val python = ProcessBuilder("python3", "-c", "import PIL")
            .redirectErrorStream(true)
            .start()
        if (python.waitFor() != 0) {
            logger.warn(
                "[generateIcons] Pillow not available; using committed icon webps. " +
                    "Run `pip install --user Pillow` to enable regeneration.",
            )
            // Skip the actual exec by replacing args with a no-op.
            commandLine("python3", "-c", "pass")
        }
    }
}

tasks.named("preBuild") { dependsOn(generateIcons) }
