package wtf.hahn.neo4j.model;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

import java.util.ArrayList;
import java.util.List;

public class Shortcuts {
    public static final String SHORTCUT_PREFIX = "sc_";
    public static final String IN_RELATION = "in_relation";
    public static final String OUT_RELATION = "out_relation";
    public static final String WEIGHT_PROPERTY_KEY = "weight_property_key";

    public static Relationship create(Relationship in, Relationship out, String costProperty, Double weight) {
        RelationshipType type = shortcutRelationshipType(in.getType());
        Relationship shortcut = in.getStartNode().createRelationshipTo(out.getEndNode(), type);
        shortcut.setProperty(IN_RELATION, in.getElementId());
        shortcut.setProperty(OUT_RELATION, out.getElementId());
        shortcut.setProperty(costProperty, weight);
        shortcut.setProperty(WEIGHT_PROPERTY_KEY, costProperty);
        return shortcut;
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
            Relationship in = transaction.getRelationshipByElementId(getProperty(relationship, IN_RELATION));
            Relationship out = transaction.getRelationshipByElementId(getProperty(relationship, OUT_RELATION));
            resolveRelationships(in, collect, transaction);
            resolveRelationships(out, collect, transaction);
        } else {
            collect.add(relationship);
        }
    }
}
