# Self try

Build the jar from the project root:

```bash
./gradlew jar
```

Change into the _procedure_ project:

```bash
cd procedure
```

Start the Neo4j docker container with the deployed plugin:

```bash
./startContainer.sh
```

## Rome Street Network

Import the street network of Rome:

```cypher
LOAD CSV FROM 'http://www.diag.uniroma1.it//~challenge9/data/rome/rome99.gr' AS line FIELDTERMINATOR ' '
WITH line WHERE line[0] = 'a'
MERGE (u:Location {name: ("V" + line[1]), id: toInteger(line[1])})
MERGE (v:Location {name: ("V" + line[2]), id: toInteger(line[2])})
MERGE (u)-[:ROAD {cost: toInteger(line[3])}]->(v)
```

Build the Contraction Hierarchies index:

```cypher
CALL wtf.hahn.neo4j.procedure.createContractionHierarchiesIndex('ROAD', 'cost')
```

Make a point to point dijkstra query:

```cypher
MATCH (a:Location {id: 2788}), (b:Location {id: 2910})
CALL wtf.hahn.neo4j.dijkstra.procedure.sourceTargetNative(a, b, 'ROAD', 'cost')
YIELD pathCost, path
RETURN pathCost, path
```

Make a point to point CH query:

```cypher
MATCH (a:Location {id: 2788}), (b:Location {id: 2910})
CALL wtf.hahn.neo4j.procedure.sourceTargetCH(a, b, 'ROAD', 'cost')
YIELD pathCost, path
RETURN pathCost, path
```