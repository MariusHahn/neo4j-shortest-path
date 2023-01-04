package wtf.hahn.neo4j.contractionHierarchies;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.util.IterationHelper;

public record Shortcut(
        RelationshipType type,
        Node start,
        Node end,
        Relationship in,
        Relationship out,
        Double weight,
        String weightPropertyName
) implements RelationshipType {

    public static final String SHORTCUT_PREFIX = "sc_";
    public static final String IN_RELATION = "in_relation";
    public static final String OUT_RELATION = "out_relation";
    public static final String WEIGHT_PROPERTY_KEY = "weight_property_key";

    public Shortcut(RelationshipType type, Node start, Node end, String weightPropertyName, WeightedPath path) {
        this(type, start, end, getRelationships(path)[0], getRelationships(path)[1], path.weight(), weightPropertyName);
    }

    public Shortcut(Relationship relationship, Transaction transaction) {
        this(
                relationship.getType()
                , relationship.getStartNode()
                , relationship.getEndNode()
                , transaction.getRelationshipByElementId((String) relationship.getProperty(IN_RELATION))
                , transaction.getRelationshipByElementId((String) relationship.getProperty(OUT_RELATION))
                , ((Number) relationship.getProperty((String) relationship.getProperty(WEIGHT_PROPERTY_KEY))).doubleValue()
                , (String) relationship.getProperty(WEIGHT_PROPERTY_KEY)
        );
    }

    private static Relationship[] getRelationships(WeightedPath path) {
        return IterationHelper.stream(path.relationships()).toArray(Relationship[]::new);
    }

    public void create() {
        Relationship relationship = start.createRelationshipTo(end, shortcutRelationshipType(in.getType()));
        relationship.setProperty(IN_RELATION, in.getElementId());
        relationship.setProperty(OUT_RELATION, out.getElementId());
        relationship.setProperty(weightPropertyName, weight);
        relationship.setProperty(WEIGHT_PROPERTY_KEY, weightPropertyName);
    }

    @Override
    public String name() {
        return shortcutRelationshipType(type).name();
    }

    public static RelationshipType shortcutRelationshipType(RelationshipType relationshipType) {
        return isShortcut(relationshipType)
                ? relationshipType
                : RelationshipType.withName(SHORTCUT_PREFIX + relationshipType.name());
    }

    public static String rankPropertyName(RelationshipType relationshipType) {
        return relationshipType.name() + "_rank";
    }

    public static boolean isShortcut(Relationship relationship) {
        return isShortcut(relationship.getType());
    }

    public static boolean isShortcut(RelationshipType type) {
        return type.name().startsWith(SHORTCUT_PREFIX);
    }
}
