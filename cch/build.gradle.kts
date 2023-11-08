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
    implementation(project(":dijkstra"))
    testImplementation(project(":test-util"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors()).takeIf { it > 0 } ?: 1
    systemProperty("junit.jupiter.execution.parallel.enabled", true)
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    jvmArgs = listOf("-Xms4096m", "-Xmx4096m")
}