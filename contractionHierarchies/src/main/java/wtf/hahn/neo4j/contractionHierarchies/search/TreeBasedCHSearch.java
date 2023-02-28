package wtf.hahn.neo4j.contractionHierarchies.search;

import static org.neo4j.graphdb.traversal.InitialBranchState.*;
import static org.neo4j.internal.helpers.MathUtil.DEFAULT_EPSILON;
import static wtf.hahn.neo4j.util.EntityHelper.getDoubleProperty;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.DijkstraSelectorFactory;
import org.neo4j.graphalgo.impl.util.PathInterestFactory;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import wtf.hahn.neo4j.contractionHierarchies.expander.ContractionHierarchiesExpander;
import wtf.hahn.neo4j.model.WeightedPathImpl;
import wtf.hahn.neo4j.util.Iterables;

public class TreeBasedCHSearch {
    private final EvaluationContext context;
    private final RelationshipType type;
    private final String rankProperty;
    private final String costProperty;
    private final CostEvaluator<Double> costEvaluator;

    public TreeBasedCHSearch(EvaluationContext context, RelationshipType type, String rankProperty, String costProperty) {
        this.context = context;
        this.type = type;
        this.rankProperty = rankProperty;
        this.costProperty = costProperty;
        costEvaluator = (relationship, direction) -> getDoubleProperty(relationship, costProperty);
    }

    public WeightedPath find(Node start, Node end) {
        TraversalDescription upToAll = traversalDescription(ContractionHierarchiesExpander.upwards(type, rankProperty));
        TraversalDescription downToAll = traversalDescription(ContractionHierarchiesExpander.downwards(type, rankProperty));

        BidirectionalTraversalDescription bidirectionalTraversalDescription =
                context.transaction().bidirectionalTraversalDescription()
                        .startSide(upToAll)
                        .endSide(downToAll);

        return Iterables.stream(bidirectionalTraversalDescription.traverse(start, end))
                .map(path -> new WeightedPathImpl(costEvaluator, path))
                .sorted().findFirst().orElse(null);
    }

    private TraversalDescription traversalDescription(ContractionHierarchiesExpander type) {
        return context.transaction().traversalDescription()
                .order(new DijkstraSelectorFactory(PathInterestFactory.allShortest(DEFAULT_EPSILON), costEvaluator))
                .expand(type, DOUBLE_ZERO)
                .evaluator(new SumCostEvaluator(costProperty))
                .uniqueness(Uniqueness.NODE_PATH);
    }

    private static class SumCostEvaluator extends PathEvaluator.Adapter<Double> {
        private final String costProperty;
        SumCostEvaluator(String costProperty) {
            this.costProperty = costProperty;
        }
        @Override
        public Evaluation evaluate(Path path, BranchState<Double> state) {
            if (path.lastRelationship() != null) {
                state.setState(state.getState() + getDoubleProperty(path.lastRelationship(), costProperty));
            }
            return Evaluation.INCLUDE_AND_CONTINUE;
        }
    }
}