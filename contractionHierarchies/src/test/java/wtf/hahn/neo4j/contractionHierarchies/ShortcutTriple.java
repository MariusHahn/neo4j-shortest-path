package wtf.hahn.neo4j.contractionHierarchies;

import org.neo4j.graphdb.Relationship;
import static wtf.hahn.neo4j.util.EntityHelper.getNameProperty;
import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

record ShortcutTriple(String start, String end, Double weight) {
    ShortcutTriple(Relationship relationship) {
        this(
                getNameProperty(relationship.getStartNode())
                , getNameProperty(relationship.getEndNode())
                , getProperty(relationship, getProperty(relationship,Shortcut.WEIGHT_PROPERTY_KEY))
        );
    }
}
