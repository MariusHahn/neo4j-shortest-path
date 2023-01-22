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
    implementation(project(":contractionHierarchies"))
    implementation(project(":dijkstra"))
    implementation(project(":model"))
    implementation(project(":util"))
    testImplementation(project(":test-util"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations.implementation.get().isCanBeResolved = true
    val localDependencies = configurations.implementation.get()
        .filter { it.name.endsWith("jar") }
        .map{zipTree(it)}
    from(localDependencies)
}