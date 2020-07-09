package com.google.sps.data;

import com.google.common.graph.*;
import com.google.gson.Gson;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;
import com.google.common.graph.EndpointPair;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.google.protobuf.Struct;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import org.json.JSONObject;

public final class Utility {

  private Utility() {
    // Should not be called
  }

  /*
   * Converts a proto node object into a graph node object that does not store the
   * names of the child nodes but may store additional information.
   *
   * @param thisNode the input data Node object
   *
   * @return a useful node used to construct the Guava Graph
   */
  public static GraphNode protoNodeToGraphNode(Node thisNode) {
    List<String> newTokenList = new ArrayList<>();
    newTokenList.addAll(thisNode.getTokenList());
    Struct newMetadata = Struct.newBuilder().mergeFrom(thisNode.getMetadata()).build();
    return GraphNode.create(thisNode.getName(), newTokenList, newMetadata);
  }

  /*
   * Converts a Guava graph into a String encoding of a JSON Object. The object
   * contains nodes, edges, and the roots of the graph.
   *
   * @param graph the graph to convert into a JSON String
   */
  public static String graphToJson(MutableGraph<GraphNode> graph, HashSet<String> roots) {
    Type typeOfNode = new TypeToken<Set<GraphNode>>() {}.getType();
    Type typeOfEdge = new TypeToken<Set<EndpointPair<GraphNode>>>() {}.getType();
    Type typeOfRoots = new TypeToken<Set<String>>() {}.getType();
    Gson gson = new Gson();
    String nodeJson = gson.toJson(graph.nodes(), typeOfNode);
    String edgeJson = gson.toJson(graph.edges(), typeOfEdge);
    String rootsJson = gson.toJson(roots, typeOfRoots);
    String allJson =
        new JSONObject()
            .put("nodes", nodeJson)
            .put("edges", edgeJson)
            .put("roots", rootsJson)
            .toString();
    return allJson;
  }

  /*
   * Changes the graph according to the given mutation object. The parameters are
   * mutated in place.
   *
   * @param mut the mutation to affect
   * @param graph the Guava graph to mutate
   * @param graphNodesMap a reference of existing nodes, also to be mutated
   * @param roots the roots of the graph before the mutation. Changed if
   * necessary.
   *
   * @return true if the mutation was successful, false otherwise
   */
  public static boolean mutateGraph(
      Mutation mut,
      MutableGraph<GraphNode> graph,
      Map<String, GraphNode> graphNodesMap,
      HashSet<String> roots) {
    // Nodes affected by the mutation
    // second node only applicable for adding an edge and removing an edge
    String startName = mut.getStartNode();
    String endName = mut.getEndNode();

    // Getting the corresponding graph nodes from the graph map
    GraphNode startNode = graphNodesMap.get(startName);
    GraphNode endNode = graphNodesMap.get(endName);

    switch (mut.getType()) {
      case ADD_NODE:
        if (graphNodesMap.containsKey(startName)) {
          // adding a duplicate node
          return true;
        }
        // New lone node is a root
        roots.add(startName);
        // Create a new node with the given name and add it to the graph and the map
        GraphNode newGraphNode =
            GraphNode.create(startName, new ArrayList<>(), Struct.newBuilder().build());
        graph.addNode(newGraphNode);
        graphNodesMap.put(startName, newGraphNode);
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
        graph.removeEdge(startNode, endNode);
        // If the target now has no in-edges, it becomes a root
        if (graph.inDegree(endNode) == 0) {
          roots.add(endName);
        }
        break;
      case CHANGE_TOKEN:
        if (startNode == null) {
          return false;
        }
        return changeNodeToken(startNode, mut.getTokenChange());
      default:
        // unrecognized mutation type
        return false;
    }
    return true;
  }

  /*
   * Modify the list of tokens for graph node 'node' to accomodate the mutation
   * 'tokenMut'. This could involve adding or removing tokens from the list.
   *
   * @param node the node in the graph to change the tokens of
   * @param tokenMut the kind of mutation to perform on node of the graph
   *
   * @return true if the change is successful, false otherwise
   */
  private static boolean changeNodeToken(GraphNode node, TokenMutation tokenMut) {
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
      return false;
    }
    return true;
  }

  /**
   * Alternative function for calculating maxDepth
   *
   * @param graphInput the input graph, as a Mutatable Graph
   * @param roots the name (string) of the roots
   * @param graphNodesMap a mapping of strings to GraphNodes
   * @param maxDepth the maximum depth of a node from a root
   * @return a graph with nodes only a certain distance from a root
   */
  public static MutableGraph<GraphNode> getGraphWithMaxDepth(
      MutableGraph<GraphNode> graphInput,
      Set<String> roots,
      HashMap<String, GraphNode> graphNodesMap,
      int maxDepth) {

    MutableGraph<GraphNode> graphToReturn = GraphBuilder.directed().build();
    if (maxDepth < 0) {
      return graphToReturn; // If max depth below 0, then return an emtpy graph
    }

    Map<GraphNode, Boolean> visited = new HashMap<>();

    for (String rootName : roots) {
      GraphNode rootNode = graphNodesMap.get(rootName);
      dfsVisit(rootNode, graphInput, visited, graphToReturn, maxDepth);
    }
    for (EndpointPair<GraphNode> edge : graphInput.edges()) {
      if (graphToReturn.nodes().contains(edge.nodeU())
          && graphToReturn.nodes().contains(edge.nodeV())) {
        graphToReturn.putEdge(edge.nodeU(), edge.nodeV());
      }
    }

    return graphToReturn;
  }

  /**
   * Helper function for calculating max depth that actually visits a node and its children
   *
   * @param gn the GraphNode to visit
   * @param graphInput the input graph
   * @param visited a map that records whether nodes have been visited
   * @param graphToReturn the graph to return, with all nodes within the specified depth
   * @param depthRemaining the number of layers left to explore, decreases by one with each
   *     recursive call on a child
   */
  private static void dfsVisit(
      GraphNode gn,
      MutableGraph<GraphNode> graphInput,
      Map<GraphNode, Boolean> visited,
      MutableGraph<GraphNode> graphToReturn,
      int depthRemaining) {
    if (depthRemaining >= 0) {
      visited.put(gn, true);
      graphToReturn.addNode(gn);
      for (GraphNode child : graphInput.successors(gn)) {
        if (!visited.containsKey(child)) {
          // Visit the child and indicate the increase in depth
          dfsVisit(child, graphInput, visited, graphToReturn, depthRemaining - 1);
        }
      }
    }
  }
}
