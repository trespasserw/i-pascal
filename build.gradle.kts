import org.jetbrains.gradle.ext.*

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.6.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.10"
}

group = "com.siberika"

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
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Plugin.Java)
    }
    implementation(project(":jps-plugin"))
    testImplementation("junit:junit:4.+")
    testImplementation("org.mockito:mockito-core:5.+")
}

sourceSets {
    main {
        java.srcDir("plugin/src")
        java.srcDir("plugin/gen")
        resources.srcDir("plugin/resources")
    }
    test {
        java.srcDir("plugin/test")
    }
}

idea {
    module {
        generatedSourceDirs.add(file("plugin/gen"))
        settings {
            packagePrefix["plugin/src"] = "com.siberika.idea.pascal"
        }
    }
}

tasks {
    generateLexer {
        sourceFile = file("plugin/src/lang/lexer/pascal.flex")
        targetOutputDir = file("plugin/gen/com/siberika/idea/pascal/lang/lexer")
    }
    generateParser {
        sourceFile = file("plugin/src/lang/parser/pascal.bnf")
        targetRootOutputDir = file("plugin/gen")
        pathToParser = "com/siberika/idea/pascal/lang/parser/PascalParser.java"
        pathToPsiRoot = "com/siberika/idea/pascal/lang/psi"
        purgeOldFiles = true
    }
    withType<JavaCompile> {
        dependsOn(generateLexer, generateParser)
        sourceCompatibility = p("javaVersion")
        targetCompatibility = p("javaVersion")
    }
}
