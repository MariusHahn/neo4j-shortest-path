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
                        Direction.INCOMING).reverse()
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

    private static int getRankProperty(Node node, String rankProperty) {
        return (int) node.getProperty(rankProperty);
    }

    private static PathExpander<Double> baseExpander(RelationshipType relationshipType,
                                                     Predicate<Relationship> filter, Direction direction) {
        /* TODO: Currently it works on every relationship type. This should change only to the given and
                 its shortcut type.
         */
        return PathExpanderBuilder
                //.empty()
                .allTypes(direction)
                .addRelationshipFilter(filter)
                //.add(relationshipType, direction)
                //.add(Shortcut.shortcutRelationshipType(relationshipType), direction)
                .build();
    }

    public enum Way {UPWARDS, DOWNWARDS}
}
