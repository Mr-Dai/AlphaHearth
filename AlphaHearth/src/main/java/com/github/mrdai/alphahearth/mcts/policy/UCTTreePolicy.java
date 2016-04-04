package com.github.mrdai.alphahearth.mcts.policy;

import com.github.mrdai.alphahearth.mcts.Node;

/**
 * {@link TreePolicy} which selects the child with the highest Upper Confidence Bound.
 * <p>
 * The constructor of this class can receive a parameter {@code cp}, which stands for the exploration factor
 * of the UCT formulation. Assigning it with a greater value emphasizes the exploration of the MCTS, while
 * setting it to a lesser value emphasizes the exploitation. It is set to {@code 1 / sqrt(2)} by default.
 */
public class UCTTreePolicy implements TreePolicy {
    private final double cp;

    /**
     * Creates a {@code UCTTreePolicy} with its Exploration Factor setting to {@code 1 / sqrt(2)}.
     */
    public UCTTreePolicy() {
        this(1 / Math.sqrt(2));
    }

    /**
     * Creates a {@code UCTTreePolicy} with the given Exploration Factor.
     */
    public UCTTreePolicy(double cp) {
        this.cp = cp;
    }

    @Override
    public Node bestChild(Node node) {
        double currentMax = Double.MIN_VALUE;
        int nodeIdx = 0;
        for (int i = 0; i < node.visitedChildren.size(); i++) {
            Node currentChild = node.visitedChildren.get(i);
            double uct = currentChild.reward / currentChild.gameCount
                         + 2 * cp * Math.sqrt(2 * Math.log(node.gameCount) / currentChild.gameCount);
            if (uct > currentMax) {
                currentMax = uct;
                nodeIdx = i;
            }
        }

        return node.visitedChildren.get(nodeIdx);
    }
}