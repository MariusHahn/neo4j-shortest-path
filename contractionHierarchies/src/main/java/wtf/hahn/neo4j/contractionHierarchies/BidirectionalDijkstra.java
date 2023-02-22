package wtf.hahn.neo4j.contractionHierarchies;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;

import org.eclipse.collections.impl.factory.Sets;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
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

    public WeightedPath findShortestPath(final Node startNode, final Node endNode, final String costProperty) {
        final Transaction transaction = evaluationContext.transaction();
        final TraversalDescription frowardDescription = getBaseDescription(transaction).expand(upwardsExpander);
        final TraversalDescription backwardDescription = getBaseDescription(transaction).expand(upwardsExpander.reverse());
        final Map<Node, ExpandNode> forwardHeap = new HashMap<>();
        final Map<Node, ExpandNode> backwardHeap = new HashMap<>();
        final PriorityQueue<ExpandNode> forwardQueue = new PriorityQueue<>();
        final PriorityQueue<ExpandNode> backwardQueue = new PriorityQueue<>();
        forwardQueue.offer(new ExpandNode(null, startNode, 0.0));
        backwardQueue.offer(new ExpandNode(null, endNode, 0.0));
        while (
                (!forwardQueue.isEmpty() || !backwardQueue.isEmpty())
                && (!forwardHeap.containsKey(endNode) || !backwardHeap.containsKey(startNode))
                && !commonDistanceIsShortest(forwardQueue, backwardQueue, forwardHeap, backwardHeap)
        ) {
            if ((!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) && forwardQueue.peek().distance() < backwardQueue.peek().distance()
                    || !forwardQueue.isEmpty()) {
                expand(costProperty, frowardDescription, forwardHeap, forwardQueue);
            } else {
                expand(costProperty, backwardDescription, backwardHeap, backwardQueue);
            }
        }
        if (forwardHeap.containsKey(endNode)) {
            return resolvePath(startNode, endNode, forwardHeap);
        } else if (backwardHeap.containsKey(startNode)) {
            return resolvePath(endNode, startNode, backwardHeap);
        }
        return resolvePath(startNode, endNode, forwardHeap, backwardHeap);
    }

    private static boolean commonDistanceIsShortest(PriorityQueue<ExpandNode> forQ, PriorityQueue<ExpandNode> backQ, final Map<Node, ExpandNode> h1, final Map<Node, ExpandNode> h2) {
        return Sets.intersect(h1.keySet(), h2.keySet()).stream().findFirst().map(commonNode -> {
            double minDistForward = forQ.stream().mapToDouble(ExpandNode::distance).min().orElse(Double.MAX_VALUE);
            double minDistBackward = backQ.stream().mapToDouble(ExpandNode::distance).min().orElse(Double.MAX_VALUE);
            double commonDistance = h1.get(commonNode).distance() + h2.get(commonNode).distance();
            return minDistForward >= commonDistance && minDistBackward >= commonDistance;
        }).orElse(false);

    }

    private static void expand(String costProperty, TraversalDescription description,
                               Map<Node, ExpandNode> heap, PriorityQueue<ExpandNode> queue) {
        final ExpandNode expandNode = queue.poll();
        final Traverser traverse = description.traverse(expandNode.node);
        for (Path path : traverse) {
            if (path.length() == 0) continue;
            final Number cost = getProperty(path.lastRelationship(), costProperty);
            final Double distance = expandNode.distance + cost.doubleValue();
            final ExpandNode e = new ExpandNode(path.lastRelationship(), path.endNode(), distance);
            queue.offer(e);
            System.out.println(e);
            updateHeap(heap, distance, e);
        }
    }

    private static TraversalDescription getBaseDescription(Transaction transaction) {
        return transaction.traversalDescription()
                .depthFirst()
                .evaluator(Evaluators.toDepth(1));
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

    private static PathImpl.Builder resolvePathBuilder(final Node startNode, final Node endNode, final Map<Node, ExpandNode> heap) {
        final Stack<ExpandNode> relationships = new Stack<>();
        ExpandNode expandNode = heap.get(endNode);
        while (expandNode != null && expandNode.in != null && !relationships.contains(expandNode)) {
            relationships.push(expandNode);
            expandNode = heap.get(expandNode.in.getStartNode());
        }
        PathImpl.Builder builder = new PathImpl.Builder(startNode);
        while (!relationships.empty()){
            ExpandNode pop = relationships.pop();
            builder = builder.push(pop.in);
        }
        return builder;
    }

    private static WeightedPath resolvePath(final Node startNode, final Node endNode,
                                            final Map<Node, ExpandNode> forwardHeap,
                                            final Map<Node, ExpandNode> backwardHeap) {
        return Sets.intersect(forwardHeap.keySet(), backwardHeap.keySet()).stream().findFirst().map(common -> {
            PathImpl.Builder left = resolvePathBuilder(startNode, common, forwardHeap);
            PathImpl.Builder right = resolvePathBuilder(endNode, common, backwardHeap);
            PathImpl pathBuild = left.build(right);
            return new WeightedPathImpl(forwardHeap.get(common).distance + backwardHeap.get(common).distance,
                    pathBuild);

        }).orElse(null);
    }

    private static WeightedPath resolvePath(final Node startNode, final Node endNode, final Map<Node, ExpandNode> heap) {
        PathImpl.Builder builder = resolvePathBuilder(startNode, endNode, heap);
        return new WeightedPathImpl(heap.get(endNode).distance(), builder.build());
    }

    private record ExpandNode(Relationship in, Node node, Double distance) implements Comparable<ExpandNode> {
        @Override
        public int compareTo(ExpandNode o) {
            return distance.compareTo(o.distance);
        }
    }
}
