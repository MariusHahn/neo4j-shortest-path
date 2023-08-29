package wtf.hahn.neo4j.cch.storage;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wft.hahn.neo4j.cch.storage.ArcReader;
import wft.hahn.neo4j.cch.storage.ArcWriter;
import wft.hahn.neo4j.cch.storage.DiskArc;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.PositionWriter;

public class ArcReadWriterTest {

    @TempDir private static Path basePath;

    @Test
    void writeReadArcsTest() throws Exception {
        Map<Integer, List<DiskArc>> arcGroup = Files.lines(Paths.get("src", "test", "resources", "rome99.csv"))
                .map(line -> Arrays.stream(line.split(",")))
                .map(stream -> stream.map(String::trim).toArray(String[]::new))
                .map(line -> new DiskArc(parseInt(line[0]), parseInt(line[1]), -1, parseFloat(line[2])))
                .filter(diskArc -> diskArc.start() < diskArc.end())
                .collect(Collectors.groupingBy(DiskArc::start));
        int sum = arcGroup.values().stream().mapToInt(Collection::size).sum();
        try (ArcWriter arcWriter = new ArcWriter(Mode.OUT, basePath);
             PositionWriter positionWriter = new PositionWriter(Mode.OUT, basePath, sum)) {
            arcGroup.forEach((rank, arcs) -> positionWriter.write(rank, arcWriter.write(arcs)));
        }
        try (ArcReader arcReader = new ArcReader(Mode.OUT, basePath)){
            arcGroup.forEach((rank, arcs) -> {
                List<DiskArc> loadArcs = arcReader.getArcs(rank);
                for (DiskArc arc : arcs) {
                    Assertions.assertTrue(loadArcs.contains(arc), "arc: %s not found".formatted(arc));
                }
            });
        }
    }

    //@Test
    void outEdgesToText() throws IOException {
        edgesToText("OUT.cch");
    }

    //@Test
    void inEdgesToText() throws IOException {
        edgesToText("IN.cch");
    }

    private void edgesToText(String fileName) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(Path.of(fileName)));
        for (int i = 0; buffer.position() < buffer.capacity(); i++) {
            DiskArc edge = new DiskArc(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getFloat());
            System.out.printf("%d: %s%n", i / 256, edge);
        }
    }
}
