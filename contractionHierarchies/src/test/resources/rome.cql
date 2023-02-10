LOAD CSV FROM 'http://www.diag.uniroma1.it//~challenge9/data/rome/rome99.gr' AS line FIELDTERMINATOR ' '
WITH line WHERE line[0] = 'a'
MERGE (u:Location {name: ("V" + line[1]), id: toInteger(line[1])})
MERGE (v:Location {name: ("V" + line[2]), id: toInteger(line[2])})
MERGE (u)-[:ROAD {cost: toInteger(line[3])}]->(v)