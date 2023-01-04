package wtf.hahn.neo4j.contractionHierarchies;

import org.neo4j.graphdb.Relationship;

record ShortcutTriple(String start, String end, Double weight) {
    ShortcutTriple(Relationship relationship) {
        this(
                (String) relationship.getStartNode().getProperty("name")
                , (String) relationship.getEndNode().getProperty("name")
                , (Double) relationship.getProperty((String) relationship.getProperty(Shortcut.WEIGHT_PROPERTY_KEY))
        );
    }
}
