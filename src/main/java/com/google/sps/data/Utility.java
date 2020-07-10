package com.google.sps.data;

import com.google.common.graph.*;
import com.google.gson.Gson;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.Mutation;
import com.google.common.graph.EndpointPair;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import com.google.protobuf.Struct;
import java.util.HashSet;
import java.util.Set;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import org.json.JSONObject;

public final class Utility {

  private Utility() {
    // Should not be called
  }

  /**
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

  /**
   * Converts a Guava graph into a String encoding of a JSON Object. The object
   * contains nodes, edges, and the roots of the graph.
   *
   * @param graph the graph to convert into a JSON String
   * @param roots the roots of the graph to convert into a JSON String
   *
   * @return a JSON object containing as entries the nodes, edges and roots of this
   * graph
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

  /**
   * Returns a mutable shallow copy of the given guava graph
   *
   * @param graph the guava graph to shallow copy
   *
   * @return a shallow copy of the given guava graph
   */
  public static MutableGraph<GraphNode> copyGraph(MutableGraph graph) {
    return Graphs.copyOf(graph);
  }

  /**
   * Returns a mutable deep copy of the set of strings
   *
   * @param roots the set of strings to deep copy

   * @return a deep copy of the given set of strings
   */
  public static HashSet<String> copyRoots(HashSet<String> roots) {
    HashSet<String> copy = new HashSet<>();
    copy.addAll(roots);
    return copy;
  }

   /**
   * Returns a mutable deep copy of the given map from string to graph node
   *
   * @param graphNodesMap the map from string to graph node to deepcopy
   *
   * @return a deep copy of the given map from string to Graph Node
   */
  public static HashMap<String, GraphNode> copyNodeMap(HashMap<String, GraphNode> graphNodesMap) {
    HashMap<String, GraphNode> copy = new HashMap<>();
    for (String key : graphNodesMap.keySet()) {
      copy.put(key, graphNodesMap.get(key).getCopy());
    }
    return copy;
  }
}
