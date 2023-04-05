package wtf.hahn.neo4j.contractionHierarchies.index;

import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedInNodes;
import static wtf.hahn.neo4j.contractionHierarchies.index.IndexUtil.getNotContractedOutNodes;
import static wtf.hahn.neo4j.util.PathUtils.samePath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import wtf.hahn.neo4j.contractionHierarchies.search.NativeDijkstra;
import wtf.hahn.neo4j.contractionHierarchies.expander.NodeIncludeExpander;
import wtf.hahn.neo4j.model.Shortcuts;
import wtf.hahn.neo4j.model.inmemory.GraphLoader;
import wtf.hahn.neo4j.util.Iterables;

public final class ContractionHierarchiesIndexerByNodeDegree implements ContractionHierarchiesIndexer {
    private final RelationshipType type;
    private final String costProperty;
    private final NativeDijkstra dijkstra;
    private final String rankPropertyName;
    private final GraphLoader graphLoader;


    public ContractionHierarchiesIndexerByNodeDegree(String type, String costProperty, Transaction transaction, GraphDatabaseService databaseService) {
        this.type = RelationshipType.withName(type);
        this.costProperty = costProperty;
        dijkstra = new NativeDijkstra(new BasicEvaluationContext(transaction, databaseService));
        rankPropertyName = Shortcuts.rankPropertyName(this.type);
        graphLoader = new GraphLoader(transaction);
    }

    record XShortcut(Relationship in, Relationship out , Double weight) {}
    Collection<XShortcut> shortCutsToInsert(Node nodeToContract) {
        List<XShortcut> shortcutsToInsert = new ArrayList<>();
        Node[] notContractedInNodes = getNotContractedInNodes(type, nodeToContract);
        Node[] notContractedOutNodes = getNotContractedOutNodes(type, nodeToContract);
        for (Node inNode : notContractedInNodes) {
            for (Node outNode : notContractedOutNodes) {
                if (inNode.equals(outNode)) continue;
                Iterable<WeightedPath> shortestPaths = dijkstra.shortestPathsWithShortcuts(inNode, outNode, type, costProperty, rankPropertyName);
                NodeIncludeExpander includeExpander = new NodeIncludeExpander(nodeToContract, type, rankPropertyName);
                WeightedPath includePath = dijkstra.shortestPath(inNode, outNode, includeExpander, costProperty);
                if (Iterables.stream(shortestPaths).anyMatch(path -> samePath(path, includePath))) {
                    Iterator<Relationship> relationshipIterator = includePath.relationships().iterator();
                    shortcutsToInsert.add(new XShortcut(relationshipIterator.next(), relationshipIterator.next(), includePath.weight()));
                }
            }
        }
        return shortcutsToInsert;
    }

    record ContractionPair(Collection<XShortcut> shortcutsToInsert, Node node){}
    ContractionPair findNodeToContract(Map<Node,Node> nodes) {
        return nodes.keySet().stream()
                .sorted(Comparator.comparingInt(Node::getDegree))
                .map(node -> new ContractionPair(shortCutsToInsert(node), node))
                .findFirst().orElseThrow();
    }

    public int insertShortcuts() {
        int insertionCounter = 0;
        Set<Node> nodes = graphLoader.loadAllNodes(type);
        Map<Node, Node> queue = nodes.stream().collect(Collectors.toMap(x -> x, x -> x));
        int rank = 0;
        while (!queue.isEmpty()) {
            ContractionPair nodeToContract = findNodeToContract(queue);
            queue.remove(nodeToContract.node);
            for (XShortcut shortcut : nodeToContract.shortcutsToInsert) {
                Shortcuts.create(shortcut.in, shortcut.out, costProperty, shortcut.weight);
                insertionCounter++;
            }
            nodeToContract.node.setProperty(rankPropertyName, rank++);
        }
        graphLoader.saveAllNode(nodes);
        return insertionCounter;
    }
}
