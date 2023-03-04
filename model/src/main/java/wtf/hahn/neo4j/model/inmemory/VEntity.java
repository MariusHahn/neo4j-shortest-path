package wtf.hahn.neo4j.model.inmemory;

import static wtf.hahn.neo4j.model.inmemory.Modification.DELETED;
import static wtf.hahn.neo4j.model.inmemory.Modification.MODIFIED;
import static wtf.hahn.neo4j.model.inmemory.Modification.NONE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.neo4j.graphdb.Entity;

public class VEntity implements Entity {

    private final Long id;
    private final String elementId;
    private final Map<String, ObservableProperty> properties;
    protected Modification modification;

    public VEntity() {
        this.id = null;
        this.elementId = null;
        this.properties = new HashMap<>();
        this.modification = MODIFIED;
    }

    public VEntity(Entity entity) {
        id = entity.getId();
        elementId = entity.getElementId();
        properties = entity.getAllProperties().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey
                , it -> new ObservableProperty(NONE, it.getValue())
        ));
        this.modification = NONE;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    @Override
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key).property;
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        return properties.containsKey(key) ? getProperty(key) : defaultValue;
    }

    @Override
    public void setProperty(String key, Object value) {
        modification = MODIFIED;
        properties.put(key, new ObservableProperty(MODIFIED, value));
    }

    @Override
    public Object removeProperty(String key) {
        if(!hasProperty(key)) {
            throw new IllegalStateException("Cannot remove property %s because it does not exit".formatted(key));
        }
        modification = MODIFIED;
        ObservableProperty old = properties.get(key);
        return properties.put(key, new ObservableProperty(DELETED, old.property));
    }

    @Override
    public Iterable<String> getPropertyKeys() {
        return properties.keySet();
    }

    @Override
    public Map<String, Object> getProperties(String... keys) {
        Set<String> keyz = Arrays.stream(keys).collect(Collectors.toSet());
        return properties.entrySet().stream()
                .filter(entry -> keyz.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().property));
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().property));
    }

    @Override
    public void delete() {
        throw new NotImplementedException("Cannot delete in Memory Entity");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VEntity vEntity = (VEntity) o;
        return Objects.equals(elementId, vEntity.elementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId);
    }

    public Modification modification() {
        return modification;
    }

    public Stream<Map.Entry<String, ObservableProperty>> getProperties(Modification modification) {
        return properties.entrySet().stream()
                .filter(entry -> entry.getValue().modification.equals(modification));
    }

    record ObservableProperty(Modification modification, Object property){}
}
