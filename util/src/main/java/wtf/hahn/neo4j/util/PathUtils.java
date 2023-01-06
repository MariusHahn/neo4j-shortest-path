package wtf.hahn.neo4j.util;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;

public class PathUtils {
    public static boolean samePath(WeightedPath shortestPath, WeightedPath includePath) {
        if (shortestPath.length() != includePath.length()) {
            return false;
        }
        if (shortestPath.weight() != includePath.weight()) {
            return false;
        }
        Iterator<Relationship> shortestPathIterator = shortestPath.relationships().iterator();
        Iterator<Relationship> includePathIterator = includePath.relationships().iterator();
        while (shortestPathIterator.hasNext() && includePathIterator.hasNext()) {
            if (!shortestPathIterator.next().equals(includePathIterator.next())) {
                return false;
            }
        }
        return true;
    }
}
