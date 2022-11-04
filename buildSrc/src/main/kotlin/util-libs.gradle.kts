plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly( "org.projectlombok:lombok:${Versions.lombok}")
    annotationProcessor( "org.projectlombok:lombok:${Versions.lombok}")
    testCompileOnly( "org.projectlombok:lombok:${Versions.lombok}")
    testAnnotationProcessor( "org.projectlombok:lombok:${Versions.lombok}")
}