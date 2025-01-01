package com.namnv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.namnv.util.ConfigUtils.getClusterNode;


public class ClusterMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMain.class);


    public static void main(final String[] args) {
        final int nodeID = args.length > 0 ? Integer.parseInt(args[0]) : getClusterNode();
        final int maxNodes = args.length > 0 ? Integer.parseInt(args[1]) : 1;
        final boolean test = args.length > 0 && Boolean.parseBoolean(args[2]);
        LOGGER.info("Attempting to start cluster node: [NodeID: " + nodeID + "] | [MaxNodes: " + maxNodes + "] | [Test: " + test + "]");
        new ClusterNode().startNode(nodeID, maxNodes, test);
    }
}
