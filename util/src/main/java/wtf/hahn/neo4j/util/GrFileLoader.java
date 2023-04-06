package wtf.hahn.neo4j.util;

import lombok.RequiredArgsConstructor;

import static java.lang.Integer.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class GrFileLoader {
    private final Path filePath;

    public Stream<Integer> getAllNodeIds() throws IOException {
        return grLines()
                .flatMap(grLine -> Stream.of(grLine.startId, grLine.endId))
                .distinct();
    }

    record GrLine(Integer startId, Integer endId, Integer distance) {}
    public Stream<GrLine> grLines() throws IOException {
        return Files.lines(filePath)
                .map(String::trim)
                .filter(line -> line.startsWith("a"))
                .map(line -> line.split(" "))
                .map(line -> new GrLine(valueOf(line[1]), valueOf(line[2]), valueOf(line[3])))
                ;
    }
}
