plugins {
    java
    application
    id(Plugins.utilLibs)
}

group = "wtf.hahn.neo4j.cchExperiments"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cch"))
    implementation(project(":util"))
    implementation(project(":dijkstra"))

    // https://mvnrepository.com/artifact/org.neo4j/neo4j
    implementation("org.neo4j:neo4j:${Versions.neo4j}")
}

application {
    mainClass.set("wtf.hahn.neo4j.cchExperiments.Application")
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Xms4096m", "-Xmx4096m")
}

tasks.register<Jar>("uberJar") {
    manifest.attributes["Main-Class"] = "wtf.hahn.neo4j.cchExperiments.Application"
    archiveClassifier.set("uber")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    val jars = configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }
    from(jars.map { zipTree(it) })
}
