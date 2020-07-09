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

  /**
   * @param original the original graph
   * @param mutationNum number of mutations to apply
   * @param graphNodesMap
   * @param mutList mutation list
   * @param roots roots, to modify
   */
  public static boolean getGraphAtMutationNumber(DataGraph original, DataGraph curr, int mutationNum, List<Mutation> mutList) {
      boolean success = true;
      if (curr.getMutationNum() <= mutationNum) { // going forward
        for (int i = curr.getMutationNum(); i < mutationNum; i++) {
          
          // Mutate graph operates in place
          success = curr.mutateGraph(mutList.get(i));
          if (!success) {
            break;
          }
        }
      } else {
        // Create a copy of the original graph and start from the original graph
        DataGraph originalCopy = original.getCopy();
        for (int i = 0; i < mutationNum; i ++) {
          success = originalCopy.mutateGraph(mutList.get(i));
          if (!success) {
            break;
          }
        }
        curr = originalCopy; 
      }
      return success;
  }
}
