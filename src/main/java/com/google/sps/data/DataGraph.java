package com.google.sps.data;

import com.google.common.graph.*;
import com.proto.GraphProtos.Node;
import java.util.HashMap;
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
  public DataGraph() {
    this.graph = GraphBuilder.directed().build();
    this.graphNodesMap = new HashMap<>();
    this.roots = new HashSet<>();
  }

  /**
   * Getter for the graph
   *
   * @return the graph
   */
  public MutableGraph<GraphNode> getGraph() {
    return this.graph;
  }

  /**
   * Getter for the roots
   *
   * @return a copy of the roots
   */
  public HashSet<String> getRoots() {
    HashSet<String> copy = new HashSet<>();
    for (String s : this.roots) {
      copy.add(s);
    }
    return copy;
  }

  /**
   * Getter for the nodes map
   *
   * @return a copy of the nodes map
   */
  public HashMap<String, GraphNode> getGraphNodesMap() {
    HashMap<String, GraphNode> copy = new HashMap<>();
    for (String key : this.graphNodesMap.keySet()) {
      copy.put(key, this.graphNodesMap.get(key));
    }
    return copy;
  }

  /**
   * Takes in a map from node name to proto-parsed node object. Populates graph with node and edge
   * information and graphNodesMap with links from node names to graph node objects.
   *
   * @param protNodesMap map from node name to proto Node object parsed from input
   * @param graph Guava graph to fill with node and edge information
   * @param graphNodesMap map object to fill with node-name -> graph node object links
   * @param roots the roots of the graph
   * @return false if an error occurred, true otherwise
   */
  public boolean graphFromProtoNodes(Map<String, Node> protoNodesMap) {

    for (String nodeName : protoNodesMap.keySet()) {
      Node thisNode = protoNodesMap.get(nodeName);

      // Convert thisNode into a graph node that may store additional information
      GraphNode graphNode = Utility.protoNodeToGraphNode(thisNode);

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
        GraphNode childNode = Utility.protoNodeToGraphNode(protoNodesMap.get(child));
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
}
