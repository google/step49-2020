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
import java.util.HashSet;

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
   * @param maxMutations the length of the list of mutations
   * @param mutDiff the difference between the current graph and the requested graph
   * @return a JSON object containing as entries the nodes and edges of this graph as well as the
   *     length of the list of mutations this graph is an intermediate result of applying
   */
  public static String graphToJson(
      MutableGraph<GraphNode> graph,
      List<Integer> mutationIndices,
      MultiMutation mutDiff,
      int currIndex) {
    Type typeOfNode = new TypeToken<Set<GraphNode>>() {}.getType();
    Type typeOfEdge = new TypeToken<Set<EndpointPair<GraphNode>>>() {}.getType();
    Type typeOfIndices = new TypeToken<List<Integer>>() {}.getType();
    Gson gson = new Gson();
    String nodeJson = gson.toJson(graph.nodes(), typeOfNode);
    String edgeJson = gson.toJson(graph.edges(), typeOfEdge);
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
            .put("currIndex", currIndex)
            .toString();
    return resultJson;
  }

  /**
   * @param original the original graph
   * @param curr the current (most recently-requested) graph (requires that original != curr)
   * @param mutationNum number of mutations to apply
   * @param multiMutList multi-mutation list
   * @return the resulting data graph or null if there was an error
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
      return DataGraph.create(curr.graph(), curr.graphNodesMap(), curr.roots(), mutationNum);
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
          originalCopy.graph(), originalCopy.graphNodesMap(), originalCopy.roots(), mutationNum);
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
  public static MultiMutation getDiffBetween(List<MultiMutation> multiMutList, int index) {
    if (index < 0 || index >= multiMutList.size()) {
      return null;
    }
    return multiMutList.get(index);
  }
  /**
   * Returns a list of the indices of the relevant
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
   * Finds the INDEX of the element in searchList that is strictly GREATER than tgt in a SORTED list
   *
   * @param searchList a list of integers to search through. Assumes it's sorted
   * @param tgt the number to find the next biggest number from
   * @return the INDEX of the next greater number. -1 if none
   */
  public static int getNextGreatestNumIndex(List<Integer> searchList, int tgt) {
    int start = 0;
    int end = searchList.size() - 1;

    int ans = -1;
    while (start <= end) {
      int mid = (start + end) / 2;
      // tgt is not less, so gotta go to the right
      if (searchList.get(mid) <= tgt) {
        start = mid + 1;
      }
      // go to the left otherwise
      else {
        ans = mid;
        end = mid - 1;
      }
    }
    if (ans == -1) return -1;
    return ans;
  }

  /**
   * 
   */
  public static MultiMutation filterMultiMutationByNodes(MultiMutation mm, Set<GraphNode> nodes, String filteredNodeName) {
    if (mm == null ) {
      return mm;
    }
    HashSet<String> nodeNames = new HashSet<>();
    for (GraphNode node : nodes) {
      nodeNames.add(node.name());
    }
    List<Mutation> mutList = mm.getMutationList();
    ArrayList<Mutation> lst = new ArrayList<>();
    for (Mutation mut : mutList) {
      String startName = mut.getStartNode();
      String endName = mut.getEndNode();
      if (nodeNames.contains(startName) || nodeNames.contains(endName) || filteredNodeName.equals(startName) || filteredNodeName.equals(endName)) {
        lst.add(mut);
      }
    }
    return MultiMutation.newBuilder().addAllMutation(lst).setReason(mm.getReason()).build();
  }
}
