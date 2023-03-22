package wtf.hahn.neo4j.contractionHierarchies;

import static java.util.List.of;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.testUtil.IntegrationTest;

public class RomePerformanceTest2 extends IntegrationTest {


    private final String costProperty = dataset.costProperty;
    private final String edgeLabel = dataset.relationshipTypeName;
    private static final String CONNECTION_STRING = "jdbc:sqlite:rome.sqlite";

    public RomePerformanceTest2() {
        super(of(), of(), of(), TestDataset.ROME);
        initDb();
    }

    @Test
    void randomPaths() throws SQLException {
        long timestamp = System.currentTimeMillis();
        try (Transaction transaction = database().beginTx();
             Connection connection = DriverManager.getConnection(CONNECTION_STRING)) {
            TimeResult<Integer> timeNeeded =
                    stoppedResult(() -> new ContractionHierarchiesIndexerByNodeDegree(edgeLabel, costProperty, transaction, database()).insertShortcuts());
            transaction.commit();
            String sql = "INSERT INTO contractionInsights " +
                    "(`timestamp`, `method`, contractionTime, shortcutsInserted) " +
                    "VALUES (?,?,?,?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, timestamp);
            ps.setString(2, "NodeDegree");
            ps.setLong(3, timeNeeded.executionTime);
            ps.setInt(4, timeNeeded.result);
            Random random = new Random(1);
            connection.setAutoCommit(false);
            String sqlString = "INSERT INTO result (`timestamp`, `from`,`to`,dijkstraLength,chLength,weight,dijkstraTime,chTime) values (?,?,?,?,?,?,?,?);";
            PreparedStatement statement = connection.prepareStatement(sqlString);
            random.ints(200, 1, 3353)
                    .mapToObj(i -> random.ints(200, 1, 3353).mapToObj(j -> new int[] {i, j}))
                    .flatMap(Function.identity())
                    .parallel()
                    .forEach(x -> searchPerformanceTest(x[0], x[1], statement, timestamp));
            connection.commit();
        }
    }

    void searchPerformanceTest(Integer startNodeId, Integer endNodeId, PreparedStatement statement, long timestamp) {
        try (Transaction transaction = database().beginTx()) {
            ContractionHierarchies chFinder = new ContractionHierarchies(database(), transaction);
            Node start = transaction.findNode(() -> "Location", "id", startNodeId);
            Node end = transaction.findNode(() -> "Location", "id", endNodeId);
            TimeResult<WeightedPath>
                    dijkstraPath = stoppedResult(() -> new NativeDijkstra(new BasicEvaluationContext(transaction, database())).shortestPath(start,end, PathExpanders.forTypeAndDirection(relationshipType(), Direction.OUTGOING),costProperty()));
            if (dijkstraPath.result != null) {
                TimeResult<WeightedPath>
                        chPath = stoppedResult(() -> (WeightedPath) chFinder.sourceTargetCH(start, end, edgeLabel, costProperty).findFirst().get().path);
                Assertions.assertEquals(dijkstraPath.result.weight(), chPath.result.weight());
                statement.setLong(1, timestamp);
                statement.setInt(2, startNodeId);
                statement.setInt(3, endNodeId);
                statement.setInt(4, dijkstraPath.result.length());
                statement.setInt(5, chPath.result.length());
                statement.setDouble(6, dijkstraPath.result.weight());
                statement.setLong(7, dijkstraPath.executionTime);
                statement.setLong(8, chPath.executionTime);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    record TimeResult<T>(Long executionTime, T result) {}

    private static <T> TimeResult<T> stoppedResult(Supplier<T> s) {
        long start = System.currentTimeMillis();
        T result = s.get();
        return new TimeResult<>(System.currentTimeMillis() - start, result);
    }


    private static void initDb() {
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING)) {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS result(\n" +
                    "    `timestamp` INTEGER,\n" +
                    "    `from` INTEGER,\n" +
                    "    `to` INTEGER,\n" +
                    "    dijkstraLength INTEGER,\n" +
                    "    chLength INTEGER,\n" +
                    "    weight REAL,\n" +
                    "    dijkstraTime INTEGER,\n" +
                    "    chTime INTEGER\n" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS contractionInsights(\n" +
                    "     `timestamp` INTEGER, \n" +
                    "     `method`  TEXT, \n" +
                    "     contractionTime INTEGER, \n" +
                    "     shortcutsInserted INTEGER \n" +
                    ");"
            );
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
