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

    implementation("org.neo4j.test:neo4j-harness:${Versions.neo4j}")
    implementation("org.neo4j.driver:neo4j-java-driver:${Versions.neo4j}")
}

application {
    mainClass.set("wtf.hahn.neo4j.cchExperiments.Application")
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Xms12288m", "-Xmx12288m")
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
