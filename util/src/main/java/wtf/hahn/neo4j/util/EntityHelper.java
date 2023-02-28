package wtf.hahn.neo4j.util;

import org.neo4j.graphdb.Entity;

public class EntityHelper {

    public static long getLongProperty(Entity entity, String propertyName) {
        Object property = entity.getProperty(propertyName);
        if (property instanceof Long p) return p;
        if (property instanceof Integer p) return p;
        throw new IllegalStateException("Cannot cast %s to Long because of its type %s".formatted(propertyName, property.getClass().getName()));
    }

    public static double getDoubleProperty(Entity entity, String propertyName) {
        Object property = entity.getProperty(propertyName);
        if (property instanceof Number n) return n.doubleValue();
        throw new IllegalStateException("Cannot cast %s to Long because of its type %s".formatted(propertyName, property.getClass().getName()));
    }

    public static <T> T getProperty(Entity entity, String propertyName) {
        return (T) entity.getProperty(propertyName);
    }

    public static String getNameProperty(Entity entity) {
        return getProperty(entity, "name");
    }
}
