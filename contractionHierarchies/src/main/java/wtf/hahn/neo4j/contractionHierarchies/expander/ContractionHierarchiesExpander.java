package wtf.hahn.neo4j.contractionHierarchies.expander;

import java.util.function.Predicate;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanderBuilder;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.BranchState;
import wtf.hahn.neo4j.model.Shortcut;

public record ContractionHierarchiesExpander(PathExpander<Double> baseExpander,
                                             String rankProperty,
                                             RelationshipType relationshipType,
                                             Way way)
        implements PathExpander<Double> {


    public static ContractionHierarchiesExpander upwards(RelationshipType relationshipType, String rankProperty) {
        return new ContractionHierarchiesExpander(
                baseExpander(relationshipType, r -> hasHigherRank(r.getStartNode(), r.getEndNode(), rankProperty),
                        Direction.OUTGOING)
                , rankProperty
                , relationshipType
                , Way.UPWARDS
        );
    }

    public static ContractionHierarchiesExpander downwards(RelationshipType relationshipType, String rankProperty) {
        return new ContractionHierarchiesExpander(
                baseExpander(relationshipType,
                        r -> hasHigherRank(r.getEndNode(), r.getStartNode(), rankProperty),
                        Direction.INCOMING)
                , rankProperty
                , relationshipType
                , Way.DOWNWARDS
        );
    }

    @Override
    public ResourceIterable<Relationship> expand(Path path, BranchState<Double> state) {
        return baseExpander.expand(path, state);
    }

    @Override
    public PathExpander<Double> reverse() {
        return way() == Way.UPWARDS
                ? downwards(relationshipType(), rankProperty())
                : upwards(relationshipType(), rankProperty());
    }

    private static boolean hasHigherRank(Node low, Node high, String rankProperty) {
        return getRankProperty(low, rankProperty) < getRankProperty(high, rankProperty);
    }

    private static long getRankProperty(Node node, String rankProperty) {
        Object rank = node.getProperty(rankProperty);
        return rank instanceof Long l ? l : ((Integer) rank).longValue();
    }

    private static PathExpander<Double> baseExpander(RelationshipType relationshipType,
                                                     Predicate<Relationship> filter, Direction direction) {
        return PathExpanderBuilder
                .empty()
                .addRelationshipFilter(filter)
                .add(relationshipType, direction)
                .add(Shortcut.shortcutRelationshipType(relationshipType), direction)
                .build();
    }

    public enum Way {UPWARDS, DOWNWARDS}
}
