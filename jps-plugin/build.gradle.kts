import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
}

private fun p(propertyName: String): String = providers.gradleProperty(propertyName).get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(version = p("platformVersion"))
        bundledPlugin("com.intellij.java")
    }
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = p("javaVersion")
        targetCompatibility = p("javaVersion")
    }
}
