package com.google.sps.data;

// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;

// import com.google.appengine.repackaged.com.google.protobuf.Struct;
// import com.google.common.graph.*;
// import com.google.common.graph.Graph;

// import com.google.common.graph.MutableGraph;
// import com.proto.GraphProtos.Node;

// import com.google.sps.data.GraphNode;
// import com.proto.GraphProtos.*;

import com.google.common.graph.*;
import com.proto.GraphProtos.Graph;
import com.proto.GraphProtos.Node;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.google.protobuf.Struct;
import java.util.Map;
import java.util.HashSet;

public class DataGraph {
  private MutableGraph<GraphNode> graph;
  private HashMap<String, GraphNode> graphNodesMap;
  private HashSet<String> roots;

  /**
   * Sets all the variables based on a protograph
   *
   * @param protoGraph the protograph to construct Guava Graph from
   */
  public DataGraph(Graph protoGraph) {
    Map<String, Node> protoNodesMap = protoGraph.getNodesMapMap();
    graph = GraphBuilder.directed().build();
    graphNodesMap = new HashMap<>();

    roots = new HashSet<>();
    graphFromProtoNodes(protoNodesMap);
  }

  public MutableGraph<GraphNode> getGraph() {
    return this.graph;
  }

  public HashSet<String> getRoots() {
    return this.roots;
  }

  public HashMap<String, GraphNode> getGraphNodesMap() {
    return this.graphNodesMap;
  }

  /*
   * Takes in a map from node name to proto-parsed node object. Populates graph
   * with node and edge information and graphNodesMap with links from node names
   * to graph node objects.
   *
   * @param protNodesMap map from node name to proto Node object parsed from input
   *
   * @param graph Guava graph to fill with node and edge information
   *
   * @param graphNodesMap map object to fill with node-name -> graph node object
   * links
   *
   * @param roots the roots of the graph
   *
   * @return false if an error occurred, true otherwise
   */
  public boolean graphFromProtoNodes(Map<String, Node> protoNodesMap) {

    for (String nodeName : protoNodesMap.keySet()) {
      Node thisNode = protoNodesMap.get(nodeName);

      // Convert thisNode into a graph node that may store additional information
      GraphNode graphNode = protoNodeToGraphNode(thisNode);

      // Update graph data structures to include the node as long as it doesn't
      // already exist
      if (!graphNodesMap.containsKey(nodeName)) {
        roots.add(nodeName);
        graph.addNode(graphNode);
        graphNodesMap.put(nodeName, graphNode);
      }

      // Add dependency edges to the graph
      for (String child : thisNode.getChildrenList()) {
        // This child can no longer be a root since it has an in-edge
        roots.remove(child);
        GraphNode childNode = protoNodeToGraphNode(protoNodesMap.get(child));
        if (!graphNodesMap.containsKey(child)) {
          // If child node is not already in the graph, add it
          graph.addNode(childNode);
          graphNodesMap.put(child, childNode);
        } else if (graph.hasEdgeConnecting(childNode, graphNode)) {
          // the graph is not a DAG, so we error out
          return false;
        }
        graph.putEdge(graphNode, childNode);
      }
    }
    return true;
  }

  /*
   * Converts a proto node object into a graph node object that does not store the
   * names of the child nodes but may store additional information.
   *
   * @param thisNode the input data Node object
   *
   * @return a useful node used to construct the Guava Graph
   */
  public GraphNode protoNodeToGraphNode(Node thisNode) {
    List<String> newTokenList = new ArrayList<>();
    newTokenList.addAll(thisNode.getTokenList());
    Struct newMetadata = Struct.newBuilder().mergeFrom(thisNode.getMetadata()).build();
    return GraphNode.create(thisNode.getName(), newTokenList, newMetadata);
  }
}
