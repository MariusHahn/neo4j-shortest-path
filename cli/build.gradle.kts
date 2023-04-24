plugins {
    java
    application
    id(Plugins.utilLibs)
}

version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.41.0.1")
    implementation(project(":contractionHierarchies"))
    implementation(project(":util"))
    implementation(project(":dijkstra"))
    implementation(project(":model"))
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-core:1.4.6")
    implementation("org.neo4j.test:neo4j-harness:${Versions.neo4j}")
    implementation("org.neo4j.driver:neo4j-java-driver:${Versions.neo4j}")
    implementation("ch.qos.logback:logback-classic:1.4.6")
}
application {
    mainClass.set("Application")
}


tasks.register<Jar>("uberJar") {
    manifest.attributes["Main-Class"] = "Application"
    archiveClassifier.set("uber")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    val jars = configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }
    from(jars.map { zipTree(it) })
}