package wtf.hahn.neo4j.contractionHierarchies;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

@RequiredArgsConstructor @Getter
public enum TestDataset implements IntegrationTest.DatasetEnum {
    SEMINAR_PAPER("seminar_paper_graph.cql", "cost", "ROAD"),
    ROME("rome.cql", "cost", "ROAD"),
    OLDENBURG("oldenburg.cql", "cost", "ROAD"),
    DIJKSTRA_SOURCE_TARGET_SAMPLE("neo4j_dijkstra_source_target_sample.cql", "cost", "ROAD");
    private final String fileName, costProperty, relationshipTypeName;
}
