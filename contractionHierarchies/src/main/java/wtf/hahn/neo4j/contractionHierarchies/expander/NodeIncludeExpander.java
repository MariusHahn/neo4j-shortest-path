package wtf.hahn.neo4j.contractionHierarchies.expander;

import static org.neo4j.graphdb.Direction.OUTGOING;

import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanderBuilder;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.BranchState;
import wtf.hahn.neo4j.model.Shortcut;

public record NodeIncludeExpander(Node include, PathExpander<Double> expander)
        implements PathExpander<Double> {

    public NodeIncludeExpander(Node includeNode, RelationshipType relationshipType, String rankPropertyName) {
        this(
                includeNode
                , PathExpanderBuilder
                        .empty()
                        .add(relationshipType, OUTGOING)
                        .add(Shortcut.shortcutRelationshipType(relationshipType), OUTGOING)
                        .addNodeFilter(node -> !node.hasProperty(rankPropertyName))
                        .addRelationshipFilter(relationship -> containsNode(relationship, includeNode)).build()
        );
    }

    @Override
    public ResourceIterable<Relationship> expand(Path path, BranchState<Double> state) {
        return expander.expand(path, state);
    }

    private static boolean containsNode(Relationship relationship, Node include) {
        return List.of(relationship.getNodes()).contains(include);
    }

    @Override
    public PathExpander<Double> reverse() {
        return new NodeIncludeExpander(include, expander.reverse());
    }
}