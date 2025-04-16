plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

// 添加获取Git commit信息的函数
fun getGitCommitInfo(): Map<String, String> {
    val result = HashMap<String, String>()
    
    try {
        // 获取最新commit的SHA值
        val hashProcess = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start()
        val gitHash = hashProcess.inputStream.bufferedReader().use { it.readText() }.trim()
        result["commitHash"] = gitHash
        
        // 获取commit时间
        val dateProcess = ProcessBuilder("git", "log", "-1", "--format=%ci").start()
        val gitDate = dateProcess.inputStream.bufferedReader().use { it.readText() }.trim()
        result["commitDate"] = gitDate
        
        // 获取commit信息
        val msgProcess = ProcessBuilder("git", "log", "-1", "--format=%s").start()
        val gitMsg = msgProcess.inputStream.bufferedReader().use { it.readText() }.trim()
        result["commitMessage"] = gitMsg
        
        // 获取branch名称
        val branchProcess = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD").start()
        val gitBranch = branchProcess.inputStream.bufferedReader().use { it.readText() }.trim()
        result["branch"] = gitBranch
    } catch (e: Exception) {
        // 获取失败时提供默认值
        result["commitHash"] = "unknown"
        result["commitDate"] = "unknown"
        result["commitMessage"] = "unknown"
        result["branch"] = "unknown"
    }
    
    return result
}

android {
    namespace = "com.example.jerrycan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jerrycan"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 添加Git commit信息到BuildConfig中
        val gitInfo = getGitCommitInfo()
        buildConfigField("String", "GIT_COMMIT_HASH", "\"${gitInfo["commitHash"]}\"")
        buildConfigField("String", "GIT_COMMIT_DATE", "\"${gitInfo["commitDate"]}\"")
        buildConfigField("String", "GIT_COMMIT_MESSAGE", "\"${gitInfo["commitMessage"]}\"")
        buildConfigField("String", "GIT_BRANCH", "\"${gitInfo["branch"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // 启用BuildConfig生成
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // 图标扩展
    implementation("androidx.compose.material:material-icons-extended:1.6.2")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // 生命周期组件
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // 格式化时间
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}