CREATE (V0:Location {name: "V0", id: 0, ROAD_rank: 0})
     , (V1:Location {name: "V1", id: 1, ROAD_rank: 1})
     , (V2:Location {name: "V2", id: 2, ROAD_rank: 2})

     , (V0)-[:ROAD {cost: 2, last_cch_cost_while_indexing: 2}]->(V1)
     , (V0)-[:ROAD {cost: 2, last_cch_cost_while_indexing: 2, x: "x"}]->(V2)
     , (V0)<-[:ROAD {cost: 2, last_cch_cost_while_indexing: 2}]-(V1)
     , (V0)<-[:ROAD {cost: 2, last_cch_cost_while_indexing: 2, x: "x"}]-(V2)
