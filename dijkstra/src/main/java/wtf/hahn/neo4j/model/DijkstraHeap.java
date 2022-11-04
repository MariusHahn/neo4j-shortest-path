package wtf.hahn.neo4j.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Value;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class DijkstraHeap {
    private final Map<Long, Info> heap = new HashMap<>();

    public DijkstraHeap(long nodeId) {
        heap.put(nodeId, new Info(false, 0L, null));
    }

    public void setSettled(long nodeId) {
        if (!heap.containsKey(nodeId)) throw new UnsupportedOperationException();
        Info info = heap.get(nodeId);
        heap.put(nodeId, new Info(true, info.distance, info.relationship));
    }

    public void setNodeDistance(String propertyKey, Node toSettle, Relationship relationship) {
        Long edgeWeight  = (Long) relationship.getProperty(propertyKey);
        long currentDistance = getCurrentDistance(toSettle.getId());
        long endNodeId = relationship.getEndNode().getId();
        heap.put(endNodeId,new Info(false, edgeWeight + currentDistance, relationship));

    }

    public boolean isSettled(long nodeId) {
        return !(heap.get(nodeId) == null || !heap.get(nodeId).settled);
    }

    public Long getClosestNotSettled() {
        DistanceNodeId distanceNodeId = new DistanceNodeId(null, Long.MAX_VALUE);
        for (Map.Entry<Long, Info> entry : heap.entrySet()) {
            Long nodeId = entry.getKey();
            Info info = entry.getValue();
            if (!(info.settled || info.distance >= distanceNodeId.getDistance())) {
                distanceNodeId = new DistanceNodeId(nodeId, info.getDistance());
            }
        }
        return distanceNodeId.nodeId;
    }

    public long getCurrentDistance(long nodeId) {
        return heap.get(nodeId).distance;
    }

    public List<Relationship> getPath(Long endNodeId) {
        List<Relationship> ids = new ArrayList<>();
        Info info = heap.get(endNodeId);
        while (info.relationship != null) {
            ids.add(info.relationship);
            info = heap.get(info.relationship.getStartNodeId());
        }
        return ids;
    }

    @Value private static class Info { boolean settled; long distance; Relationship relationship;}
    @Value private static class DistanceNodeId{ Long nodeId, distance;}

}
