package wtf.hahn.neo4j.contractionHierarchies.expander;

import static wtf.hahn.neo4j.contractionHierarchies.expander.NodeIncludeExpander.notYetContracted;
import static wtf.hahn.neo4j.model.Shortcuts.shortcutRelationshipType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanderBuilder;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.BranchState;

public record NotContractedWithShortcutsExpander(PathExpander<Double> expander) implements PathExpander<Double> {

    public NotContractedWithShortcutsExpander(RelationshipType relationshipType, String rankPropertyName) {
        this(PathExpanderBuilder.empty()
                .add(relationshipType, Direction.OUTGOING)
                .add(shortcutRelationshipType(relationshipType), Direction.OUTGOING)
                .addNodeFilter(node -> notYetContracted(rankPropertyName, node))
                .build());
    }

    @Override
    public ResourceIterable<Relationship> expand(Path path, BranchState<Double> state) {
        return expander.expand(path, state);
    }

    @Override
    public PathExpander<Double> reverse() {
        return new NotContractedWithShortcutsExpander(expander.reverse());
    }
}
