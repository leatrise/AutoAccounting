val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val safeAppName = (readStringResource("app_name") ?: "app").replace(" ", "_")
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias
).all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.materialThemeBuilder)
    alias(libs.plugins.autoresconfig)
}

autoResConfig {
    generateClass.set(true)
    generateRes.set(false)
    generatedClassFullName.set("net.ankio.auto.util.LangList")
    generatedArrayFirstItem.set("SYSTEM")
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf(
            "Default" to "6750A4",
            "Red" to "F44336",
            "Pink" to "E91E63",
            "Purple" to "9C27B0",
            "DeepPurple" to "673AB7",
            "Indigo" to "3F51B5",
            "Blue" to "2196F3",
            "LightBlue" to "03A9F4",
            "Cyan" to "00BCD4",
            "Teal" to "009688",
            "Green" to "4FAF50",
            "LightGreen" to "8BC3A4",
            "Lime" to "CDDC39",
            "Yellow" to "FFEB3B",
            "Amber" to "FFC107",
            "Orange" to "FF9800",
            "DeepOrange" to "FF5722",
            "Brown" to "795548",
            "BlueGrey" to "607D8F",
            "Sakura" to "FF9CA8"
        )) {
            create("Material$name") {
                lightThemeFormat = "ThemeOverlay.Light.%s"
                darkThemeFormat = "ThemeOverlay.Dark.%s"
                primaryColor = "#$color"
            }
        }
    }
    // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
    // rikka.material >= 2.0.0 provides such attributes
    generatePalette = true
}

android {
    namespace = "net.ankio.auto"
    compileSdk = 36

    signingConfigs {
        create("ciRelease") {
            if (hasReleaseSigning) {
                storeFile = file(releaseKeystorePath!!)
                storeType = "pkcs12"
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                // PKCS12 keystores use the store password for the private key as well.
                keyPassword = releaseKeystorePassword
            }
        }
    }

    defaultConfig {
        applicationId = "net.ankio.auto"
        minSdk = 29
        targetSdk = 36
        versionCode = calculateVersionCode()
        versionName = "4.0.2(1602)-m1.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        flavorDimensions += "version"
        setProperty("archivesBaseName", "$safeAppName-${versionName}(${versionCode})")

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }

    buildTypes {
        getByName("debug") {
            ndk {
                abiFilters.clear()
                abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
            }
        }
        getByName("release") {
            // 开启代码压缩以启用R8优化，但通过proguard规则禁用混淆
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("ciRelease")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    packaging {
        // 普通资源（非 .so）放这里
        resources {
            excludes += listOf(
                "META-INF/*"
            )
        }

        // 如果以后要过滤 .so，改用 jniLibs.excludes += "lib/**/foo.so"
        // jniLibs { excludes += "lib/**/yourNative.so" }
    }






    androidResources {
        additionalParameters.addAll(
            listOf("--allow-reserved-package-id", "--package-id", "0x65")
        )
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

}
fun calculateVersionCode(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()
        val commitCount = output.toIntOrNull()

        if (exitCode == 0 && commitCount != null && commitCount > 0) {
            commitCount
        } else {
            1
        }
    } catch (_: Exception) {
        // Fallback keeps local/sandboxed builds usable even if .git metadata is unavailable.
        1
    }
}

fun readStringResource(name: String): String? {
    return try {
        val stringsFile = file("src/main/res/values/strings.xml")
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(stringsFile)
        val nodes = document.getElementsByTagName("string")

        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.attributes?.getNamedItem("name")?.nodeValue == name) {
                return node.textContent.trim().ifBlank { null }
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

configurations.configureEach {
    exclude("androidx.appcompat", "appcompat")
}

dependencies {


    implementation(libs.androidx.swiperefreshlayout)


    // 打包依赖
    implementation(libs.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Html转换
    implementation(libs.html.ktx)

    // gson
    implementation(libs.gson)

    // toast
    implementation(libs.toaster)
    implementation(libs.preferenceKtx)
    implementation(libs.androidx.lifecycle.service)

    // xp依赖
    compileOnly(libs.xposed)

    // flexbox
    implementation(libs.flexbox)

    // 圆角
    implementation(libs.round)


    // okhttp
    implementation(libs.okhttp)

    // Dex工具
    implementation(project(":dex"))

    // xml2json
    implementation(libs.xmltojson)

    implementation(project(":server"))
    implementation(project(":shell"))
    implementation(project(":ocr"))
    implementation(project(":tap"))

    // debug依赖
    debugImplementation(libs.leakcanary.android)



    implementation(libs.rikkaMaterial)
    implementation(libs.rikkaMaterialPreference)
    implementation(libs.about)

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.tencent.bugly:crashreport:latest.release")
    implementation("com.tencent:mmkv-static:1.3.5")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation(kotlin("reflect"))


    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))


}
