//package com.namnv.node2;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import static com.namnv.util.ConfigUtils.getClusterNode;
//
//
//public class ClusterMain2 {
//    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMain2.class);
//
//
//    public static void main(final String[] args) throws Exception {
//        final int nodeID = args.length > 0 ? Integer.parseInt(args[0]) : getClusterNode();
//        final int maxNodes = args.length > 0 ? Integer.parseInt(args[1]) : 1;
//        final boolean test = args.length > 0 && Boolean.parseBoolean(args[2]);
//        LOGGER.info("Attempting to start cluster node: [NodeID: " + nodeID + "] | [MaxNodes: " + maxNodes + "] | [Test: " + test + "]");
//        new ClusterNode2().startNode(1, 2, test);
//    }
//}
