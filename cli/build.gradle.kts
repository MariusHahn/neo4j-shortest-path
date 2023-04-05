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

tasks.jar {
    manifest {
        attributes["Main-Class"] = "Application"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations.implementation.get().isCanBeResolved = true
    val localDependencies = configurations.implementation.get()
        .filter { it.name.endsWith("jar") }
        .map{zipTree(it)}
    from(localDependencies)
}