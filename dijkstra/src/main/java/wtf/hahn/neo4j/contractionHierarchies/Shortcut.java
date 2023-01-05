package wtf.hahn.neo4j.contractionHierarchies;

import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.Iterables;

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
                , transaction.getRelationshipByElementId(getProperty(relationship, IN_RELATION))
                , transaction.getRelationshipByElementId(getProperty(relationship, OUT_RELATION))
                , getProperty(relationship, getProperty(relationship, WEIGHT_PROPERTY_KEY))
                , getProperty(relationship, WEIGHT_PROPERTY_KEY)
        );
    }

    private static Relationship[] getRelationships(WeightedPath path) {
        return Iterables.stream(path.relationships()).toArray(Relationship[]::new);
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

    public static List<Relationship> resolveRelationships(Relationship relationship, Transaction transaction) {
        List<Relationship> relationships = new ArrayList<>();
        resolveRelationships(relationship, relationships, transaction);
        return relationships;

    }

    public static void resolveRelationships(Relationship relationship, List<Relationship> collect,
                                            Transaction transaction) {
        if (isShortcut(relationship)) {
            Shortcut shortcut = new Shortcut(relationship, transaction);
            resolveRelationships(shortcut.in(), collect, transaction);
            resolveRelationships(shortcut.out(), collect, transaction);
        } else {
            collect.add(relationship);
        }
    }
}
