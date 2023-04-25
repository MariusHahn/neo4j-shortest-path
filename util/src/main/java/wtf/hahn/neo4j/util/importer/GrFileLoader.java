package wtf.hahn.neo4j.util.importer;

import lombok.RequiredArgsConstructor;

import static java.lang.Integer.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class GrFileLoader implements FileLoader {
    private final Path filePath;

    public Stream<LoadFileRelationship> loadFileRelationships() throws IOException {
        return Files.lines(filePath)
                .map(String::trim)
                .filter(line -> line.startsWith("a"))
                .map(line -> line.split(" "))
                .map(line -> new LoadFileRelationship(valueOf(line[1]), valueOf(line[2]), valueOf(line[3])))
                ;
    }
}
