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
import com.proto.MutationProtos.TokenMutation;

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
      MutableGraph<GraphNode> graph, List<Integer> mutationIndices, MultiMutation mutDiff) {
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
          original.tokenMap());
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
   * Returns a list of indices on the original list that related to a given node
   *
   * @param tokenName the token name to search for
   * @param origList the original list of mutations
   * @return a list of indices, empty if tokenName is null or if token is not changed
   */
  public static ArrayList<Integer> getMutationIndicesOfToken(
      String tokenName, List<MultiMutation> origList) {
    ArrayList<Integer> lst = new ArrayList<>();
    if (tokenName == null) {
      return lst;
    }
    for (int i = 0; i < origList.size(); i++) {
      MultiMutation multiMut = origList.get(i);
      List<Mutation> mutList = multiMut.getMutationList();
      for (Mutation mut : mutList) {
        if (mut.getType().equals(Mutation.Type.CHANGE_TOKEN)) {
          TokenMutation tokenMut = mut.getTokenChange();
          List<String> tokenNames = tokenMut.getTokenNameList();
          if (tokenNames.contains(tokenName)) {
            lst.add(i);
            break;
          }
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

  public static List<Integer> mergeSortedLists(List<List<Integer>> sortedLists) {
    int numLists = sortedLists.size();
    if (sortedLists.size() == 0) return new ArrayList<>();
    else if (sortedLists.size() == 1) {
      return sortedLists.get(0);
    } else {
      int left = numLists / 2;
      return mergeTwoLists(
          mergeSortedLists(new ArrayList<>(sortedLists.subList(0, left))),
          mergeSortedLists(new ArrayList<>(sortedLists.subList(left, sortedLists.size()))));
    }

    // We merge two lists at a time until we get one list remaining
    // while (numLists != 1) {
    // int low = 0;
    // int high = numLists - 1;
    // while (low < high) {
    // // System.out.println(mergeTwoLists(sortedLists.get(low),
    // sortedLists.get(high)));
    // System.out.println(sortedLists);
    // sortedLists.set(low, mergeTwoLists(sortedLists.get(low),
    // sortedLists.get(high)));
    // low++;
    // high--;
    // }
    // numLists = high + 1;
    // }
    // return sortedLists.get(0);
  }

  public static List<Integer> mergeTwoLists(List<Integer> l1, List<Integer> l2) {
    ArrayList<Integer> result = new ArrayList<>();
    int pointer1 = 0;
    int pointer2 = 0;

    while (pointer1 < l1.size() && pointer2 < l2.size()) {
      int smallest;
      if (l1.get(pointer1) <= l2.get(pointer2)) {
        smallest = l1.get(pointer1);
        pointer1++;
      } else {
        smallest = l2.get(pointer2);
        pointer2++;
      }
      // no duplicates
      if (result.size() == 0 || result.get(result.size() - 1) != smallest) {
        result.add(smallest);
      }
    }
    // Check individual, if the other has reached the bounds
    while (pointer1 < l1.size()) {
      if (result.size() == 0 || result.get(result.size() - 1) != l1.get(pointer1)) {
        result.add(l1.get(pointer1));
      }
      pointer1++;
    }
    while (pointer2 < l2.size()) {
      if (result.size() == 0 || result.get(result.size() - 1) != l2.get(pointer2)) {
        result.add(l2.get(pointer2));
      }
      pointer2++;
    }
    return result;
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
