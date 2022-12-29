package wtf.hahn.neo4j.util;

import org.neo4j.graphdb.Entity;

public class EntityHelper {

    public static <T> T getProperty(Entity entity, String propertyName) {
        return (T) entity.getProperty(propertyName);
    }
}
