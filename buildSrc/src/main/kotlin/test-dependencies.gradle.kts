plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.jUnit}")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.0.1")
}