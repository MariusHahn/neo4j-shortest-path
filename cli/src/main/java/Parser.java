import lombok.SneakyThrows;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.index.ContractionHierarchiesIndexer;
import wtf.hahn.neo4j.contractionHierarchies.index.IndexerByEdgeDifference;

import static wtf.hahn.neo4j.contractionHierarchies.index.IndexerByEdgeDifference.Mode.DISK;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexerByEdgeDifference.Mode.INMEMORY;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Parser {

    private final CommandLine commandLine;
    private final Option workingDirectory = Option.builder().option("w").longOpt("work-dir").hasArg(true).numberOfArgs(1)
            .optionalArg(false).desc("Specifies the database working directory").build();
    private final Option importFile = Option.builder().option("if").longOpt("import-file").hasArg(true).numberOfArgs(1)
            .desc("Specifies the path to the cypher file that will be executed after db start-up").optionalArg(true).build();
    private final Option sourceTargetCsv = Option.builder().option("st").longOpt("source-target-csv").hasArg(true)
            .numberOfArgs(1).desc("Specifies the path to the csv file which will containing source target ids. Ids are" +
                    "integers, separated by the char ','").build();
    private final Option contractionAlgorithm = Option.builder()
            .option("a").longOpt("contraction-algorithm").optionalArg(false).hasArg(true).required()
            .build();
    private final Option relationshipType = Option.builder().option("rt").longOpt("relationship-type")
            .optionalArg(false).required().hasArg(true).numberOfArgs(1).build();
    private final Option costProperty = Option.builder().option("cp").longOpt("cost-property")
            .optionalArg(false).required().hasArg(true).numberOfArgs(1).build();

    @SneakyThrows(ParseException.class) public Parser(String... args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption(workingDirectory).addOption(importFile).addOption(sourceTargetCsv)
                .addOption(contractionAlgorithm).addOption(relationshipType).addOption(costProperty);
        this.commandLine = parser.parse(options, args);
    }

    public String getWorkingDirectory() {
        return commandLine.getOptionValue(workingDirectory);
    }

    public Path getCypherLocation() {
        return Paths.get(commandLine.getOptionValue(importFile));
    }

    public Path getSourceTargetCsvLocation() {
        return Paths.get(commandLine.getOptionValue(sourceTargetCsv));
    }

    public boolean hasImportFileDefined() {
        return commandLine.hasOption(importFile);
    }

    public ContractionHierarchiesIndexer getContractionAlgorithm(String relationshipType, String costProperty, Transaction tx, GraphDatabaseService db) {
        String alg = commandLine.getOptionValue(contractionAlgorithm);
        return switch (alg) {
            case "edge-difference" -> new IndexerByEdgeDifference(relationshipType, costProperty, tx, INMEMORY);
            case "edge-difference-disk" -> new IndexerByEdgeDifference(relationshipType, costProperty, tx, DISK);
            //case "cch-edge-difference" -> "";
            //case "cch-node-degree" -> "";
            default -> throw new IllegalArgumentException("%s is not a valid algorithm option".formatted(alg));
        };
    }

    public String getContractionAlgorithmName() {
        return commandLine.getOptionValue(contractionAlgorithm);
    }

    public String getRelationshipType() {
        return commandLine.getOptionValue(relationshipType);
    }

    public String getCostProperty() {
        return commandLine.getOptionValue(costProperty);
    }

}
