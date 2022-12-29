package wtf.hahn.neo4j.contractionHierarchies.expander;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.BranchState;
import wtf.hahn.neo4j.contractionHierarchies.Shortcut;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.internal.helpers.collection.Iterables.asResourceIterable;
import static org.neo4j.internal.helpers.collection.Iterables.filter;

import java.util.function.Predicate;

public class ContractionHierarchiesExpander implements PathExpander<Double> {

    private final PathExpander<Double> dijkstraExpander;
    private final String rankProperty;
    private final Predicate<Relationship> rankFilter;
    private final RelationshipType relationshipType;
    private final ContractionHierarchiesExpander.Side side;

    public ContractionHierarchiesExpander(RelationshipType relationshipType, String rankProperty, Side side) {
        this.relationshipType = relationshipType;
        this.side = side;
        dijkstraExpander = PathExpanders.forTypesAndDirections(
                relationshipType
                , side.direction
                , Shortcut.shortcutRelationshipType(relationshipType)
                , side.direction
        );
        this.rankProperty = rankProperty;
        this.rankFilter = side == Side.UPWARDS
                ? r -> hasHigherRank(r.getStartNode(), r.getEndNode())
                : r -> hasHigherRank(r.getEndNode(), r.getStartNode())
        ;
    }

    boolean hasHigherRank(Node low, Node high) {
        return getRankProperty(low, rankProperty) < getRankProperty(high, rankProperty);
    }

    static int getRankProperty(Node node, String rankProperty) {
        return (int) node.getProperty(rankProperty);
    }

    @Override
    public ResourceIterable<Relationship> expand(Path path, BranchState<Double> state) {
        return asResourceIterable(filter(rankFilter, dijkstraExpander.expand(path, state)));
    }

    @Override
    public PathExpander<Double> reverse() {
        return new ContractionHierarchiesExpander(relationshipType, rankProperty, side.other());
    }

    public enum Side {
        UPWARDS(OUTGOING),
        DOWNWARDS(INCOMING);

        private final Direction direction;

        Side(Direction direction) {
            this.direction = direction;
        }

        Side other() {
            return this == UPWARDS ? DOWNWARDS : UPWARDS;
        }
    }
}
