package wtf.hahn.neo4j.cchExperiments;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

public class Application {

    public static final Properties PROPERTIES = new Properties();

    static String dbDirName(String fileName) {
        return fileName.substring(0, fileName.length() - 3);
    }

    public static void main(String[] args) throws IOException {
        PROPERTIES.load(new FileInputStream("application.properties"));
        final String fileName = PROPERTIES.getProperty("inputFile");
        try (Database database = new Database(fileName)) {
            final String action = PROPERTIES.getProperty("action");
            final int bufferSize = Integer.parseInt(PROPERTIES.getProperty("bufferSize"));
            switch (action) {
                case "CREATE" -> {
                    ImportAndIndex importAndIndex = new ImportAndIndex(PROPERTIES.getProperty("inputFile"), database.getDb());
                    importAndIndex.go();
                    String measureFileName = "m1-%d.csv".formatted(System.currentTimeMillis());
                    MeasureQueries measureQueries = new MeasureQueries(fileName, bufferSize, measureFileName, database.getDb());
                    measureQueries.go();
                }
                case "UPDATE" -> {
                    ChangeAndUpdate changeAndUpdate = new ChangeAndUpdate(Paths.get(dbDirName(fileName)), database.getDb());
                    changeAndUpdate.go();
                    String measureFileName = "m6%d.csv".formatted(System.currentTimeMillis());
                    MeasureQueries measureQueries = new MeasureQueries(fileName, bufferSize, measureFileName, database.getDb());
                    measureQueries.go();
                }
                   case "MEASURE" -> {
                    String measureFileName = "m%d.csv".formatted(System.currentTimeMillis());
                    MeasureQueries measureQueries = new MeasureQueries(fileName, bufferSize, measureFileName, database.getDb());
                    measureQueries.go();
                }
                default -> throw new IllegalStateException("Unexpected value: " + action);
            }
        }
    }
}