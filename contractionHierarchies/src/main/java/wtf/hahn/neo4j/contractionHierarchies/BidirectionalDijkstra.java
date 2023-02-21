package wtf.hahn.neo4j.contractionHierarchies;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;

import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

public record BidirectionalDijkstra(EvaluationContext evaluationContext, PathExpander<Double> upwardsExpander,
                                    PathExpander<Double> downwardsExpander) {

    public Path findShortestPath(final Node startNode, final Node endNode, final String costProperty) {
        final Transaction transaction = evaluationContext.transaction();
        final TraversalDescription frowardDescription = transaction.traversalDescription()
                .depthFirst()
                .evaluator(Evaluators.toDepth(1))
                .expand(upwardsExpander);
        final Map<Node, ExpandNode> forwardHeap = new HashMap<>();
        final PriorityQueue<ExpandNode> forwardQueue = new PriorityQueue<>();
        forwardQueue.offer(new ExpandNode(null, startNode, 0.0));
        while (!forwardQueue.isEmpty() && !forwardHeap.containsKey(endNode)) {
            final ExpandNode expandNode = forwardQueue.poll();
            final Traverser traverse = frowardDescription.traverse(expandNode.node);
            for (Path path : traverse) {
                if (path.length() == 0) continue;
                final Number cost = getProperty(path.lastRelationship(), costProperty);
                final Double distance = expandNode.distance + cost.doubleValue();
                final ExpandNode e = new ExpandNode(path.lastRelationship(), path.endNode(), distance);
                forwardQueue.offer(e);
                System.out.println(e);
                updateHeap(forwardHeap, distance, e);
            }
        }
        return resolvePath(startNode, endNode, forwardHeap);
    }

    private static void updateHeap(final Map<Node, ExpandNode> heap, final Double distance, final ExpandNode expandNode) {
        if (!heap.containsKey(expandNode.node)) {
            heap.put(expandNode.node, expandNode);
        } else {
            Double oldDistance = heap.get(expandNode.node).distance;
            if (oldDistance > distance) {
                heap.put(expandNode.node, expandNode);
            }
        }
    }

    private Path resolvePath(final Node startNode, final Node endNode, final Map<Node, ExpandNode> heap) {
        final Stack<ExpandNode> relationships = new Stack<>();
        ExpandNode expandNode = heap.get(endNode);
        while (expandNode != null && expandNode.in != null) {
            relationships.push(expandNode);
            expandNode = heap.get(expandNode.in.getStartNode());
        }
        PathImpl.Builder builder = new PathImpl.Builder(startNode);
        while (!relationships.empty()){
            ExpandNode pop = relationships.pop();
            builder = builder.push(pop.in);
        }
        return new WeightedPathImpl(heap.get(endNode).distance(), builder.build());
    }

    private record ExpandNode(Relationship in, Node node, Double distance) implements Comparable<ExpandNode> {
        @Override
        public int compareTo(ExpandNode o) {
            return distance.compareTo(o.distance);
        }
    }
}
