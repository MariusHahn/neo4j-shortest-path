package wtf.hahn.neo4j.dijkstra.expander;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.internal.helpers.collection.Iterables;
import wtf.hahn.neo4j.util.IterationHelper;
import static org.neo4j.graphdb.Direction.OUTGOING;

public record NodeRestrictedExpander(Node restrictedNode, PathExpander<Double> expander)
        implements PathExpander<Double> {

    public NodeRestrictedExpander(Node restrictedNode, RelationshipType relationshipType) {
        this(restrictedNode, PathExpanders.forTypeAndDirection(relationshipType, OUTGOING));
    }

    @Override
    public ResourceIterable<Relationship> expand(Path path, BranchState<Double> state) {
        if (IterationHelper.stream(path.nodes()).anyMatch(restrictedNode::equals)) {
            return Iterables.emptyResourceIterable();
        }
        return expander.expand(path, state);
    }

    @Override
    public PathExpander<Double> reverse() {
        return new NodeRestrictedExpander(restrictedNode, expander.reverse());
    }
}
