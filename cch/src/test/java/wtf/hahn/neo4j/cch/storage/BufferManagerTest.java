package wtf.hahn.neo4j.cch.storage;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

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
import wft.hahn.neo4j.cch.storage.ArcWriter;
import wft.hahn.neo4j.cch.storage.DiskArc;
import wft.hahn.neo4j.cch.storage.FifoBuffer;
import wft.hahn.neo4j.cch.storage.Mode;
import wft.hahn.neo4j.cch.storage.PositionWriter;

public class BufferManagerTest {

    @TempDir private static Path basePath;

    @Test
    void baseReadTest() throws Exception {
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
        try (FifoBuffer fifoBuffer = new FifoBuffer(1024, Mode.OUT, basePath)){
            arcGroup.forEach((rank, arcs) -> {
                System.out.println(rank);
                List<DiskArc> loadArcs = fifoBuffer.arcs(rank);
                Assertions.assertIterableEquals(arcs, loadArcs);
            });
        }
    }
}
