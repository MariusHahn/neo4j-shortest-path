LOAD CSV FROM 'file:///home/marius/vcs/neo4j-shortest-path/cli/src/main/resources/oldenburg.gr' AS line FIELDTERMINATOR ' '
MERGE (u:Location {name: ("V" + line[0]), id: toInteger(line[0])})
MERGE (v:Location {name: ("V" + line[1]), id: toInteger(line[1])})
MERGE (u)-[:ROAD {cost: toInteger(line[2])}]->(v)
MERGE (u)<-[:ROAD {cost: toInteger(line[2])}]-(v)