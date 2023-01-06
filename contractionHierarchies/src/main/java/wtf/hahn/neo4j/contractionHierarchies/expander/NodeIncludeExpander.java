package wtf.hahn.neo4j.contractionHierarchies.expander;

import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.internal.helpers.collection.Iterables.asResourceIterable;
import static org.neo4j.internal.helpers.collection.Iterables.filter;

import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.BranchState;
import wtf.hahn.neo4j.contractionHierarchies.Shortcut;

public record NodeIncludeExpander(Node include, PathExpander<Double> expander)
        implements PathExpander<Double> {

    public NodeIncludeExpander(Node includeNode, RelationshipType relationshipType) {
        this(
                includeNode
                , PathExpanders.forTypesAndDirections(
                        relationshipType
                        , OUTGOING
                        , Shortcut.shortcutRelationshipType(relationshipType)
                        , OUTGOING
                )
        );
    }

    @Override
    public ResourceIterable<Relationship> expand(Path path, BranchState<Double> state) {
        return asResourceIterable(filter(this::containsNode, expander.expand(path, state)));
    }

    private boolean containsNode(Relationship relationship) {
        return List.of(relationship.getNodes()).contains(include);
    }

    @Override
    public PathExpander<Double> reverse() {
        return new NodeIncludeExpander(include, expander.reverse());
    }
}
