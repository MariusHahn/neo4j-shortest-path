java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

plugins {
    id("java-library")
    id(Plugins.neo4jDependencies)
    id(Plugins.testDependencies)
    id(Plugins.utilLibs)
}

dependencies{
    implementation("org.junit.jupiter:junit-jupiter:${Versions.jUnit}")
    implementation("org.neo4j.test:neo4j-harness:${Versions.neo4j}")
    implementation("org.neo4j.driver:neo4j-java-driver:${Versions.neo4j}")
    implementation("com.tngtech.archunit:archunit-junit5:${Versions.archunitJunit5}")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}