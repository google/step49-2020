package com.google.sps.data;

import com.google.common.graph.*;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;
import com.proto.GraphProtos.Node;
import com.google.protobuf.Struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

public class DataGraph {
  // A directed mutable Guava graph containing the nodes and edges of this graph
  private MutableGraph<GraphNode> graph;
  // A map from the name to the node object for nodes of this graph
  private HashMap<String, GraphNode> graphNodesMap;
  // A set of names of roots (nodes with no in-edges) of this graph
  private HashSet<String> roots;
  /*
   * The index in the list of mutations upto which the original graph was mutated
   * to obtain this graph
   */
  private int mutationNum;

  // Initializes an empty data graph
  public DataGraph() {
    this.graph = GraphBuilder.directed().build();
    this.graphNodesMap = new HashMap<>();
    this.roots = new HashSet<>();
    this.mutationNum = 0;
  }

  // Initializes a data graph with the given fields
  public DataGraph(
      MutableGraph<GraphNode> graph,
      HashMap<String, GraphNode> map,
      HashSet<String> roots,
      int mutationNum) {
    this.graph = graph;
    this.graphNodesMap = map;
    this.roots = roots;
    this.mutationNum = mutationNum;
  }

  public int getMutationNum() {
    return this.mutationNum;
  }
  /**
   * Check whether the given object is equal in contents to the current data graph
   *
   * @param other the object to check is equal to this data graph
   * @return true if they are equal, false if not or if this object is not a data graph
   */
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof DataGraph)) {
      return false;
    }
    DataGraph otherGraph = (DataGraph) other;
    return this.graph.equals(otherGraph.getGraph())
        && this.roots.equals(otherGraph.getRoots())
        && this.graphNodesMap.equals(otherGraph.getGraphNodesMap());
  }

  /**
   * Returns a deep copy of the given data graph
   *
   * @return a deep copy of this data graph
   */
  public DataGraph getCopy() {
    HashMap<String, GraphNode> copyMap = new HashMap<>();
    for (String key : graphNodesMap.keySet()) {
      copyMap.put(key, graphNodesMap.get(key));
    }
    HashSet<String> copyRoots = new HashSet<>();
    copyRoots.addAll(roots);
    return new DataGraph(Graphs.copyOf(this.graph), copyMap, copyRoots, mutationNum);
  }

  /**
   * Getter for the graph
   *
   * @return a shallow copy of the graph
   */
  public MutableGraph<GraphNode> getGraph() {
    // return Graphs.copyOf(this.graph);
    return this.graph;
  }

  /**
   * Getter for the roots
   *
   * @return a deep copy of the roots
   */
  public HashSet<String> getRoots() {
    return this.roots;
  }

  /**
   * Getter for the nodes map
   *
   * @return a deep copy of the nodes map
   */
  public HashMap<String, GraphNode> getGraphNodesMap() {
    return this.graphNodesMap;
  }

  /**
   * Takes in a map from node name to proto-parsed node object. Populates fields of this data graph
   * with this information
   *
   * @param protNodesMap map from node name to proto Node object parsed from input
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

  /**
   * Applies a single mutation to the given data graph
   *
   * @param mut the mutation to apply to the graph
   * @return true if the mutation was successfully applied, false otherwise
   */
  public boolean mutateGraph(Mutation mut) {
    // Nodes affected by the mutation
    // second node only applicable for adding an edge and removing an edge
    String startName = mut.getStartNode();
    String endName = mut.getEndNode();

    // Getting the corresponding graph nodes from the graph map
    GraphNode startNode = graphNodesMap.get(startName);
    GraphNode endNode = graphNodesMap.get(endName);

    switch (mut.getType()) {
      case ADD_NODE:
        // adding a duplicate node doesn't make any change
        if (!graphNodesMap.containsKey(startName)) {
          // New lone node is a root
          roots.add(startName);
          // Create a new node with the given name and add it to the graph and the map
          GraphNode newGraphNode =
              GraphNode.create(startName, new ArrayList<>(), Struct.newBuilder().build());
          graph.addNode(newGraphNode);
          graphNodesMap.put(startName, newGraphNode);
        }
        break;
      case ADD_EDGE:
        if (startNode == null || endNode == null) { // Check nodes exist before adding an edge
          return false;
        }
        // The target cannot be a root since it has an in-edge
        roots.remove(endName);
        graph.putEdge(startNode, endNode);
        break;
      case DELETE_NODE:
        if (startNode == null) { // Check node exists before removing
          return false;
        }
        // Check whether any successor will have no in-edges after this node is removed
        // If so, make them roots
        Set<GraphNode> successors = graph.successors(startNode);
        for (GraphNode succ : successors) {
          if (graph.inDegree(succ) == 1) {
            roots.add(succ.name());
          }
        }
        roots.remove(startName);
        graph.removeNode(startNode); // This will remove all edges associated with startNode
        graphNodesMap.remove(startName);
        break;
      case DELETE_EDGE:
        if (startNode == null || endNode == null) { // Check nodes exist before removing edge
          return false;
        }
        // If the target now has no in-edges, it becomes a root
        if (graph.inDegree(endNode) == 1) {
          roots.add(endName);
        }
        graph.removeEdge(startNode, endNode);
        break;
      case CHANGE_TOKEN:
        if (startNode == null) {
          return false;
        }
        GraphNode newNode = changeNodeToken(startNode, mut.getTokenChange());

        if (newNode == null) {
          return false;
        }

        graphNodesMap.put(startName, newNode);

        Set<GraphNode> s = graph.successors(startNode);
        Set<GraphNode> p = graph.predecessors(startNode);
        graph.removeNode(startNode);

        graph.addNode(newNode);
        for (GraphNode gn : s) {
          graph.putEdge(newNode, gn);
        }
        for (GraphNode gn : p) {
          graph.putEdge(gn, newNode);
        }

        break;
      default:
        // unrecognized mutation type
        return false;
    }
    this.mutationNum ++;
    return true;
  }
  /**
   * Modifies the list of tokens of this node to either add or remove tokens contained in tokenMut
   *
   * @param node the node whose token list should be modified
   * @param tokenMut the mutation that should be applied to the token list
   * @return the new GraphNode object, or null if it's an unrecognized mutation
   */
  private GraphNode changeNodeToken(GraphNode node, TokenMutation tokenMut) {

    // List of tokens to add/remove from the existing list
    List<String> tokenNames = tokenMut.getTokenNameList();
    // The existing list of tokens in the node
    List<String> tokenList = node.tokenList();
    TokenMutation.Type tokenMutType = tokenMut.getType();
    if (tokenMutType == TokenMutation.Type.ADD_TOKEN) {
      tokenList.addAll(tokenNames);
    } else if (tokenMutType == TokenMutation.Type.DELETE_TOKEN) {
      tokenList.removeAll(tokenNames);
    } else {
      // unrecognized mutation
      return null;
    }
    GraphNode newNode = GraphNode.create(node.name(), tokenList, node.metadata());
    return newNode;
  }

  /**
   * Function for calculating nodes reachable from roots of this graph within at most maxDepth steps
   *
   * @param maxDepth the maximum depth of a node from a root
   * @return a graph with nodes only a certain distance from a root
   */
  public MutableGraph<GraphNode> getGraphWithMaxDepth(int maxDepth) {
    if (maxDepth < 0) {
      return GraphBuilder.directed().build(); // If max depth below 0, then return an emtpy graph
    }

    Map<GraphNode, Boolean> visited = new HashMap<>();

    for (String rootName : roots) {
      GraphNode rootNode = graphNodesMap.get(rootName);
      dfsVisit(rootNode, visited, maxDepth);
    }
    MutableGraph<GraphNode> graphToReturn = Graphs.inducedSubgraph(graph, visited.keySet());

    return graphToReturn;
  }

  /**
   * Helper function for performing a depth-first traversal of the graph starting at node and adding
   * all those nodes to visited which are within depthRemaining steps from the node
   *
   * @param gn the GraphNode to start at
   * @param visited a map that records whether nodes have been visited
   * @param depthRemaining the number of layers left to explore, decreases by one with each
   *     recursive call on a child
   */
  private void dfsVisit(GraphNode gn, Map<GraphNode, Boolean> visited, int depthRemaining) {
    if (depthRemaining >= 0) {
      visited.put(gn, true);
      for (GraphNode child : graph.successors(gn)) {
        if (!visited.containsKey(child)) {
          // Visit the child and indicate the increase in depth
          dfsVisit(child, visited, depthRemaining - 1);
        }
      }
    }
  }
}
