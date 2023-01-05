package wtf.hahn.neo4j.dijkstra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import wtf.hahn.neo4j.util.ReverseIterator;
import static wtf.hahn.neo4j.util.EntityHelper.getProperty;

public class Dijkstra {
    private final Map<Node, Info> heap = new HashMap<>();
    private final Node endNode;
    private final String propertyKey;
    private final RelationshipType relationshipType;

    public Dijkstra(Node startNode, Node endNode, String propertyKey, RelationshipType relationshipType) {
        this.endNode = endNode;
        this.propertyKey = propertyKey;
        this.relationshipType = relationshipType;
        heap.put(startNode, new Info(false, 0L, null));
    }

    public void setSettled(Node node) {
        if (!heap.containsKey(node)) throw new UnsupportedOperationException();
        Info info = heap.get(node);
        heap.put(node, new Info(true, info.distance, info.relationship));
    }

    public void setNodeDistance(String propertyKey, Node toSettle, Relationship relationship) {
        Long edgeWeight = getProperty(relationship, propertyKey);
        long currentDistance = getCurrentDistance(toSettle);
        Node endNode = relationship.getEndNode();
        heap.put(endNode ,new Info(false, edgeWeight + currentDistance, relationship));

    }

    public boolean isSettled(Node nodeId) {
        return !(heap.get(nodeId) == null || !heap.get(nodeId).settled);
    }

    public Node getClosestNotSettled() {
        record DistanceNodeId(Node node, Long distance){}
        DistanceNodeId distanceNodeId = new DistanceNodeId(null, Long.MAX_VALUE);
        for (Map.Entry<Node, Info> entry : heap.entrySet()) {
            Node nodeId = entry.getKey();
            Info info = entry.getValue();
            if (!(info.settled || info.distance >= distanceNodeId.distance())) {
                distanceNodeId = new DistanceNodeId(nodeId, info.distance());
            }
        }
        return distanceNodeId.node;
    }

    public long getCurrentDistance(Node node) {
        return heap.get(node).distance;
    }

    public Iterator<Relationship> getRelationships() {
        List<Relationship> ids = new ArrayList<>();
        Info info = heap.get(endNode);
        while (info.relationship != null) {
            ids.add(info.relationship);
            info = heap.get(info.relationship.getStartNode());
        }
        return new ReverseIterator<>(ids);
    }

    public void calcSourceTarget() {
        while (getClosestNotSettled() != null && !isSettled(endNode)) {
            final Node toSettle = getClosestNotSettled();
            for (Relationship relationship : toSettle.getRelationships(Direction.OUTGOING, relationshipType)) {
                setNodeDistance(propertyKey, toSettle, relationship);
            }
            setSettled(toSettle);
        }
    }

    record Info (boolean settled, long distance, Relationship relationship){}
}
