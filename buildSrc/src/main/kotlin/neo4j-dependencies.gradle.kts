plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.neo4j:neo4j:${Versions.neo4j}")
    testImplementation("org.neo4j.test:neo4j-harness:${Versions.neo4j}")
    testImplementation("org.neo4j.driver:neo4j-java-driver:${Versions.neo4j}")
}