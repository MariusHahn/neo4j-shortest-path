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

dependencies {
    implementation(project(":util"))
    implementation(project(":model"))
    testImplementation(project(":test-util"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}