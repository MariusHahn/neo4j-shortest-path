
plugins {
    id("java-library")
    id(Plugins.neo4jDependencies)
    id(Plugins.testDependencies)
    id(Plugins.utilLibs)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}