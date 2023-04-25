package wtf.hahn.neo4j.util.importer;

import java.io.IOException;
import java.util.stream.Stream;

public interface FileLoader {
    Stream<LoadFileRelationship> loadFileRelationships() throws IOException;
}
