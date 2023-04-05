package wtf.hahn.neo4j.util;

import org.neo4j.graphdb.config.Setting;

public record SimpleSetting<T>(String name, T defaultValue, boolean dynamic, String description) implements Setting<T> {
    public SimpleSetting(String name, T defaultValue) {
        this(name, defaultValue, false, "");
    }
}
