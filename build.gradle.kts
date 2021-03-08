import java.util.*
import java.text.SimpleDateFormat

plugins {
    kotlin("multiplatform") version "1.4.31"
    id("com.android.application")
    id("kotlin-android-extensions")
    id("maven-publish")
}

val libName = "HNFoundation"
val libVersionName = "1.0.3"
val cocoaDestination = "$rootDir/../../hn-foundation-cocoa"

group = "me.felipe"
version = libVersionName

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(LEGACY) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    
    android()

    iosX64 {
        binaries {
            framework {
                baseName = libName
            }
        }
    }
    iosArm64 {
        binaries {
            framework {
                baseName = libName
            }
        }
    }
    iosArm32 {
        binaries {
            framework {
                baseName = libName
            }
        }
    }
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val nativeMain by getting
        val nativeTest by getting
        val androidMain by getting {
            dependencies {
                implementation("com.google.android.material:material:1.2.1")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13")
            }
        }
        val iosX64Main by getting
        val iosX64Test by getting
        val iosArm64Main by getting
        val iosArm64Test by getting
        val iosArm32Main by getting
        val iosArm32Test by getting
    }

    tasks {
        register("universalFrameworkDebug", org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask::class) {
            baseName = libName
            from(
                iosArm64().binaries.getFramework(libName, "Debug"),
                iosX64().binaries.getFramework(libName, "Debug")
            )
            destinationDir = buildDir.resolve(cocoaDestination)
            print("Destination DIR: $destinationDir")
            group = libName
            description = "Debug lib for iOS"
            dependsOn("link${libName}DebugFrameworkIosArm64")
            dependsOn("link${libName}DebugFrameworkIosX64")
        }

        register("universalFrameworkRelease", org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask::class) {
            baseName = libName
            from(
                iosArm64().binaries.getFramework(libName, "Release"),
                iosX64().binaries.getFramework(libName, "Release")
            )
            destinationDir = buildDir.resolve(cocoaDestination)
            group = libName
            description = "Create the debug fat framework for ihhhOs"
            dependsOn("link${libName}ReleaseFrameworkIosArm64")
            dependsOn("link${libName}ReleaseFrameworkIosX64")
        }

        // Faz o Build e push para ios DEV
        register("publishDevFramework") {
            description = "Publish iOs framework to the Cocoa Repo"

            project.exec {
                workingDir = File(cocoaDestination)
                commandLine("git", "checkout", "develop").standardOutput
            }

            dependsOn("universalFrameworkDebug")

            doLast {
                val dir = File("${cocoaDestination}/${libName}.podspec")
                val tempFile = File("${cocoaDestination}/${libName}.podspec.new")

                val reader = dir.bufferedReader()
                val writer = tempFile.bufferedWriter()
                var currentLine: String?

                while (reader.readLine().also { currLine -> currentLine = currLine } != null) {
                    if (currentLine?.startsWith("s.version") == true) {
                        writer.write("s.version       = \"${libVersionName}\"" + System.lineSeparator())
                    } else {
                        writer.write(currentLine + System.lineSeparator())
                    }
                }
                writer.close()
                reader.close()
                val successful = tempFile.renameTo(dir)

                if (successful) {

                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
                    project.exec {
                        workingDir = File(cocoaDestination)
                        commandLine("git", "commit", "-a", "-m", "\"New dev release: ${libVersionName}-${dateFormatter.format(Date())}\"").standardOutput
                    }

                    project.exec {
                        workingDir = File(cocoaDestination)
                        commandLine("git", "push", "origin", "develop").standardOutput
                    }
                }
            }
        }

        // Faz o Build e push para ios MASTER
        register("publishFramework") {
            description = "Publish iOs framework to the Cocoa Repo"

            project.exec {
                workingDir = File(cocoaDestination)
                commandLine("git", "checkout", "master").standardOutput
            }

            dependsOn("universalFrameworkRelease")

            doLast {
                val dir = File("${cocoaDestination}/${libName}.podspec")
                val tempFile = File("${cocoaDestination}/${libName}.podspec.new")

                val reader = dir.bufferedReader()
                val writer = tempFile.bufferedWriter()
                var currentLine: String?

                while (reader.readLine().also { currLine -> currentLine = currLine } != null) {
                    if (currentLine?.startsWith("s.version") == true) {
                        writer.write("s.version       = \"${libVersionName}\"" + System.lineSeparator())
                    } else {
                        writer.write(currentLine + System.lineSeparator())
                    }
                }
                writer.close()
                reader.close()
                val successful = tempFile.renameTo(dir)

                if (successful) {

                    project.exec {
                        workingDir = File(cocoaDestination)
                        commandLine("git", "commit", "-a", "-m", "\"New release: ${libVersionName}\"").standardOutput
                    }

                    project.exec {
                        workingDir = File(cocoaDestination)
                        commandLine("git", "tag", libVersionName).standardOutput
                    }

                    project.exec {
                        workingDir = File(cocoaDestination)
                        commandLine("git", "push", "origin", "master", "--tags").standardOutput
                    }
                }
            }
        }
    }
}

android {
    compileSdkVersion(29)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        applicationId = "me.felipe.library"
        minSdkVersion(24)
        targetSdkVersion(29)
    }
}


