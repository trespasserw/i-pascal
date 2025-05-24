plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(version = properties["platformVersion"].toString())
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
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}
