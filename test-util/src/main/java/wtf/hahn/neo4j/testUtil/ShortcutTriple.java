package wtf.hahn.neo4j.testUtil;

import org.neo4j.graphdb.Relationship;
import wtf.hahn.neo4j.model.Shortcuts;
import static wtf.hahn.neo4j.util.EntityHelper.getNameProperty;
import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

public record ShortcutTriple(String start, String end, Double weight) {
    public ShortcutTriple(Relationship relationship) {
        this(
                getNameProperty(relationship.getStartNode())
                , getNameProperty(relationship.getEndNode())
                , getProperty(relationship, getProperty(relationship, Shortcuts.WEIGHT_PROPERTY_KEY))
        );
    }
}
