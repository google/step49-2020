// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableGraph;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.protobuf.Struct;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.MultiMutation;
import com.proto.MutationProtos.Mutation;

import org.json.JSONObject;

public final class Utility {

  private Utility() {
    // Should not be called
  }

  /**
   * Converts a proto node object into a graph node object that does not store the names of the
   * child nodes but may store additional information.
   *
   * @param thisNode the input data Node object
   * @return a useful node used to construct the Guava Graph
   */
  public static GraphNode protoNodeToGraphNode(Node thisNode) {
    List<String> newTokenList = new ArrayList<>();
    newTokenList.addAll(thisNode.getTokenList());
    Struct newMetadata = Struct.newBuilder().mergeFrom(thisNode.getMetadata()).build();
    return GraphNode.create(thisNode.getName(), newTokenList, newMetadata);
  }

  /**
   * Converts a Guava graph into a String encoding of a JSON Object. The object contains nodes and
   * edges of the graph.
   *
   * @param graph the graph to convert into a JSON String
   * @param mutationIndices the list of indices of relevant mutations
   * @param mutDiff the difference between the current graph and the requested graph
   * @param maxNumber the total number of mutations, without filtering
   * @param queried a list of node names the client had requested
   * @return a JSON object containing as entries the nodes and edges of this graph as well as the
   *     length of the list of mutations this graph is an intermediate result of applying
   */
  public static String graphToJson(
      MutableGraph<GraphNode> graph,
      List<Integer> mutationIndices,
      MultiMutation mutDiff,
      int maxNumber,
      List<String> queried) {
    Type typeOfNode = new TypeToken<Set<GraphNode>>() {}.getType();
    Type typeOfEdge = new TypeToken<Set<EndpointPair<GraphNode>>>() {}.getType();
    Type typeOfIndices = new TypeToken<List<Integer>>() {}.getType();
    Type typeOfQueried = new TypeToken<List<String>>() {}.getType();
    Gson gson = new Gson();
    String nodeJson = gson.toJson(graph.nodes(), typeOfNode);
    String edgeJson = gson.toJson(graph.edges(), typeOfEdge);
    String queriedJson = gson.toJson(queried, typeOfQueried);
    String mutDiffJson =
        (mutDiff == null || !mutDiff.isInitialized()) ? "" : gson.toJson(mutDiff.getMutationList());
    String reason = (mutDiff == null || !mutDiff.isInitialized()) ? "" : mutDiff.getReason();
    String mutationIndicesJson = gson.toJson(mutationIndices, typeOfIndices);
    String resultJson =
        new JSONObject()
            .put("nodes", nodeJson)
            .put("edges", edgeJson)
            .put("mutationDiff", mutDiffJson)
            .put("reason", reason)
            .put("mutationIndices", mutationIndicesJson)
            .put("totalMutNumber", maxNumber)
            .put("queriedNodes", queriedJson)
            .toString();
    return resultJson;
  }

  /**
   * Returns the graph at the given mutation number null if the requested number is less than -1. If
   * the user requests a number greater than the total number of mutations, we return the final
   * graph.
   *
   * @param original the original graph
   * @param curr the current (most recently-requested) graph (requires that original != curr)
   * @param mutationNum number of mutations to apply
   * @param multiMutList multi-mutation list
   * @throws IllegalArgumentException if original and current graph refer to the same object
   * @return the resulting data graph, null if the mutation number was too small, and the final
   *     graph if the mutation number was too big
   */
  public static DataGraph getGraphAtMutationNumber(
      DataGraph original, DataGraph curr, int mutationNum, List<MultiMutation> multiMutList)
      throws IllegalArgumentException {
    Preconditions.checkArgument(
        original != curr, "The current graph and the original graph refer to the same object");

    if (mutationNum < -1) {
      return null;
    } else if (mutationNum > multiMutList.size()) {
      mutationNum = multiMutList.size() - 1;
    }

    if (curr.numMutations() <= mutationNum) { // going forward
      for (int i = curr.numMutations() + 1; i <= mutationNum; i++) {
        // Mutate graph operates in place
        MultiMutation multiMut = multiMutList.get(i);
        List<Mutation> mutations = multiMut.getMutationList();
        for (Mutation mut : mutations) {
          String error = curr.mutateGraph(mut);
          if (error.length() != 0) {
            throw new IllegalArgumentException(error);
          }
        }
      }
      return DataGraph.create(
          curr.graph(), curr.graphNodesMap(), curr.roots(), mutationNum, curr.tokenMap());
    } else {
      // Create a copy of the original graph and start from the original graph
      DataGraph originalCopy = original.getCopy();
      for (int i = 0; i <= mutationNum; i++) {
        MultiMutation multiMut = multiMutList.get(i);
        List<Mutation> mutations = multiMut.getMutationList();
        for (Mutation mut : mutations) {
          String error = originalCopy.mutateGraph(mut);
          if (error.length() != 0) {
            throw new IllegalArgumentException(error);
          }
        }
      }
      return DataGraph.create(
          originalCopy.graph(),
          originalCopy.graphNodesMap(),
          originalCopy.roots(),
          mutationNum,
          originalCopy.tokenMap());
    }
  }

  /**
   * Returns a multi-mutation (list of mutations) that need to be applied to get from the graph at
   * currIndex to the graph at nextIndex as long as nextIndex = currIndex + 1
   *
   * @param multiMutList the list of multi-mutations that are to be applied to the initial graph
   * @param index the index in the above list at which the multimutation to apply is
   * @return a multimutation with all the changes to apply to the current graph to get the next
   *     graph or null if the provided indices are out of bounds or non-consecutive
   */
  public static MultiMutation getMultiMutationAtIndex(List<MultiMutation> multiMutList, int index) {
    if (index < 0 || index >= multiMutList.size()) {
      return null;
    }
    return multiMutList.get(index);
  }

  /**
   * Returns a list of the indices of the mutations in origList that mutate nodeName
   *
   * @param nodeName the name of the node to filter
   * @param origList the original list of mutations
   * @return a list of indices that are relevant to the node
   */
  public static ArrayList<Integer> getMutationIndicesOfNode(
      String nodeName, List<MultiMutation> origList) {
    ArrayList<Integer> lst = new ArrayList<>();
    // Shouldn't happen, but in case the nodeName is null an empty list is returned
    if (nodeName == null) {
      return lst;
    }
    for (int i = 0; i < origList.size(); i++) {
      MultiMutation multiMut = origList.get(i);
      List<Mutation> mutList = multiMut.getMutationList();
      for (Mutation mut : mutList) {
        String startName = mut.getStartNode();
        String endName = mut.getEndNode();
        if (nodeName.equals(startName) || nodeName.equals(endName)) {
          lst.add(i);
          break;
        }
      }
    }
    return lst;
  }

  /**
   * Converts a Guava graph containing nodes of type GraphNode into a set of names of nodes
   * contained in the graph
   *
   * @param graph the graph to return node names for
   * @return a set of names of nodes in the graph
   */
  public static Set<String> getNodeNamesInGraph(MutableGraph<GraphNode> graph) {
    return graph.nodes().stream().map(node -> node.name()).collect(Collectors.toSet());
  }

  /**
   * Filters the mutations contained in this multimutation to be only the ones that affect the nodes
   * in the provided set
   *
   * @param mm the multimutation to filter
   * @param nodeNames the list of node names to return perninent mutations for
   * @return a multimutation containing only those mutations in mm affecting nodes in nodeNames,
   *     null if the multimutation is null and the multimutation itself if there is no name to
   *     filter by
   */
  public static MultiMutation filterMultiMutationByNodes(MultiMutation mm, Set<String> nodeNames) {
    if (mm == null || nodeNames.size() == 0) {
      return mm;
    }
    List<Mutation> originalMutationList = mm.getMutationList();
    ArrayList<Mutation> filteredMutationList = new ArrayList<>();
    for (Mutation mut : originalMutationList) {
      String startName = mut.getStartNode();
      String endName = mut.getEndNode();
      if (nodeNames.contains(startName) && (endName.equals("") || nodeNames.contains(endName))) {
        filteredMutationList.add(mut);
      }
    }
    return MultiMutation.newBuilder()
        .addAllMutation(filteredMutationList)
        .setReason(mm.getReason())
        .build();
  }
}
