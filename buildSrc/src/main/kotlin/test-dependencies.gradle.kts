plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.jUnit}")
    testImplementation("com.tngtech.archunit:archunit-junit5:${Versions.archunitJunit5}")
    testImplementation("org.xerial:sqlite-jdbc:3.41.0.1")
}