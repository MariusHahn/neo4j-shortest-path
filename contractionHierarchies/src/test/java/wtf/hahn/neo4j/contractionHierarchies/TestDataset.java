package wtf.hahn.neo4j.contractionHierarchies;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

@RequiredArgsConstructor @Getter
public enum TestDataset implements IntegrationTest.DatasetEnum {
    DIJKSTRA_SOURCE_TARGET_SAMPLE("neo4j_dijkstra_source_target_sample.cql", "cost", "ROAD");
    private final String fileName, costProperty, relationshipTypeName;
}
