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

tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    val jars = configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }
    from(jars.map { zipTree(it) })
}