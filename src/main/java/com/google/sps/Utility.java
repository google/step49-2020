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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
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
import com.proto.MutationProtos.MutationList;
import com.proto.MutationProtos.TokenMutation;

import org.json.JSONObject;

/** This file contains utility functions used for various tasks in the servlet */
public final class Utility {

  private Utility() {
    // Should not be called
  }

  /**
   * Converts a proto node object into a graph node object that does not store the names of the
   * child nodes but may store additional information.
   *
   * @param thisNode the input Node object
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
   * edges of the graph as well as indices of mutations that mutate graph nodes, a list of the
   * mutations that were applied in the last step to get from the old graph to this graph, the total
   * length of the mutation list and a list of queriedNodes to highlight.
   *
   * @param graph the graph to convert into a JSON String
   * @param mutationIndices the indices in the entire mutation list that mutate the relevant nodes
   * @param mutDiff the difference between the current graph and the requested graph
   * @param maxNumber the total number of mutations, without filtering
   * @param queried a set of node names the client had requested
   * @return a JSON object containing the nodes and edges of this graph, the relevant mutation
   *     indices of the node(s) the user filtered for, the difference between the current graph and
   *     requested graph, the reason for the mutation, the total number of mutations (for ALL
   *     nodes), and the nodes the user filtered for
   */
  public static String graphToJson(
      MutableGraph<GraphNode> graph,
      List<Integer> mutationIndices,
      MultiMutation mutDiff,
      int maxNumber,
      HashSet<String> queried) {
    Type typeOfNode = new TypeToken<Set<GraphNode>>() {}.getType();
    Type typeOfEdge = new TypeToken<Set<EndpointPair<GraphNode>>>() {}.getType();
    Type typeOfIndices = new TypeToken<List<Integer>>() {}.getType();
    Type typeOfQueried = new TypeToken<Set<String>>() {}.getType();
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
   * Returns the graph at the given mutation number, null if the requested number is less than -1.
   * If the user requests a number greater than the total number of mutations, we return the final
   * graph.
   *
   * @param original the original graph
   * @param curr the current (most recently-requested) graph (requires that original != curr)
   * @param mutationNum the index of the last mutation to apply
   * @param multiMutList multi-mutation list builder. This parameter may be modified by replacing
   *     some mutations with their deduplicated versions.
   * @throws IllegalArgumentException if original and current graph refer to the same object
   * @return the resulting data graph, null if the mutation number was too small, and the final
   *     graph if the mutation number was too big.
   */
  public static DataGraph getGraphAtMutationNumber(
      DataGraph original, DataGraph curr, int mutationNum, MutationList.Builder mutationsList)
      throws IllegalArgumentException {
    Preconditions.checkArgument(
        original != curr, "The current graph and the original graph refer to the same object");

    List<MultiMutation> multiMutList = mutationsList.getMutationList();

    if (mutationNum < -1) {
      return null;
    } else if (mutationNum > multiMutList.size()) {
      mutationNum = multiMutList.size() - 1;
    }
    // If the requested graph is sequentially before the current graph but is closer to
    // the initial graph than to the current graph, go forward from the initial graph rather
    // than going back from the current graph
    if (curr.numMutations() - mutationNum > mutationNum) {
      curr = original.getCopy();
    }
    if (curr.numMutations() <= mutationNum) { // going forward
      for (int i = curr.numMutations() + 1; i <= mutationNum; i++) {
        // Mutate graph operates in place
        MultiMutation multiMut = multiMutList.get(i);
        List<Mutation> mutations = multiMut.getMutationList();
        // Use this to store the multi mutation without any redundant mutation
        // information (for eg. duplicate tokens to add)
        MultiMutation.Builder trimmedMultiMut = MultiMutation.newBuilder();
        for (Mutation mut : mutations) {
          Mutation.Builder currMut = mut.toBuilder();
          String error = curr.mutateGraph(currMut);
          if (error.length() != 0) {
            throw new IllegalArgumentException(error);
          }
          trimmedMultiMut.addMutation(currMut.build());
        }
        mutationsList.setMutation(i, trimmedMultiMut.setReason(multiMut.getReason()));
      }
      return DataGraph.create(
          curr.graph(), curr.graphNodesMap(), curr.roots(), mutationNum, curr.tokenMap());
    } else {
      // The last mutation to revert is the one after the last one to apply
      for (int i = curr.numMutations(); i > mutationNum; i--) {
        // Mutate graph operates in place
        MultiMutation multiMut = revertMultiMutation(multiMutList.get(i));
        List<Mutation> mutations = multiMut.getMutationList();
        for (Mutation mut : mutations) {
          String error = curr.mutateGraph(mut.toBuilder());
          if (error.length() != 0) {
            throw new IllegalArgumentException(error);
          }
        }
      }
      return DataGraph.create(
          curr.graph(), curr.graphNodesMap(), curr.roots(), mutationNum, curr.tokenMap());
    }
  }

  /**
   * Returns the last multi-mutation (list of mutations) that needs to be applied to get from the
   * graph at currIndex to the graph at nextIndex as long as nextIndex > currIndex
   *
   * @param multiMutList the list of multi-mutations that are to be applied to the initial graph
   * @param index the index of the last-applied multimutation
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
   * Returns a set of indices on the original list that related to a given token
   *
   * @param tokenName the token name to search for
   * @param origList the original list of mutations
   * @return a set of indices, empty if tokenName is null or empty
   */
  public static Set<Integer> getMutationIndicesOfToken(
      String tokenName, List<MultiMutation> origList) {
    Set<Integer> indices = new HashSet<>();
    if (tokenName == null || tokenName.length() == 0) {
      return indices;
    }
    for (int i = 0; i < origList.size(); i++) {
      List<Mutation> mutList = origList.get(i).getMutationList();
      for (Mutation mut : mutList) {
        if (mut.getType().equals(Mutation.Type.CHANGE_TOKEN)) {
          List<String> tokenNames = mut.getTokenChange().getTokenNameList();
          if (tokenNames.contains(tokenName)) {
            indices.add(i);
            break;
          }
        }
      }
    }
    return indices;
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

  /**
   * Given a list of node names, a map from node name to mutation indices of that node and a list of
   * multimutations applied to all nodes, returns a set of indices of multimutations in which any of
   * the nodes in nodeNames get mutated. If nodeNames is empty, an empty set is returned
   *
   * @param nodeNames the names of nodes to restrict the returned list of mutations to
   * @param mutationIndicesMap a map from node name -> indices of mutations that mutate it
   * @param multiMutList a list of multimutations which mutationIndices map indexes into
   * @return a set of indices in multiMutList at which any of the nodes in nodeNames are mutated
   *     Might modify mutationIndices map by caching information about relevant mutation indices of
   *     some nodes
   */
  public static Set<Integer> findRelevantMutations(
      Collection<String> nodeNames,
      Map<String, List<Integer>> mutationIndicesMap,
      List<MultiMutation> multiMutList) {
    if (nodeNames.size() == 0) {
      return new HashSet<>();
    }
    Set<Integer> relevantIndices = new HashSet<>();
    for (String nodeName : nodeNames) {
      // Find or compute and cache the relevant mutation indices for each node
      if (!mutationIndicesMap.containsKey(nodeName)) {
        mutationIndicesMap.put(nodeName, getMutationIndicesOfNode(nodeName, multiMutList));
      }
      relevantIndices.addAll(mutationIndicesMap.get(nodeName));
    }
    return relevantIndices;
  }

  /**
   * Returns a multi-mutation which undoes the changes caused by the passed multi- mutation in the
   * opposite order to which they are made
   *
   * @param multiMut the multi-mutation to reverse
   * @return the reverted multi-mutation
   */
  public static MultiMutation revertMultiMutation(MultiMutation multiMut) {
    MultiMutation.Builder result = MultiMutation.newBuilder().setReason(multiMut.getReason());
    List<Mutation> mutations = multiMut.getMutationList();
    List<Mutation> revertedMutations = new ArrayList<>();
    for (int i = mutations.size() - 1; i >= 0; i--) {
      revertedMutations.add(revertMutation(mutations.get(i)));
    }
    return result.addAllMutation(revertedMutations).build();
  }

  /**
   * Returns a mutation which undoes the changes caused by the passed mutation. For example,
   * reverting an add edge deletes the edge.
   *
   * @param mut the mutation to reverse
   * @return the reverted mutation
   */
  public static Mutation revertMutation(Mutation mut) {
    Mutation.Builder result = Mutation.newBuilder(mut);
    switch (mut.getType()) {
      case ADD_NODE:
        {
          result.setType(Mutation.Type.DELETE_NODE);
          break;
        }
      case DELETE_NODE:
        {
          result.setType(Mutation.Type.ADD_NODE);
          break;
        }
      case ADD_EDGE:
        {
          result.setType(Mutation.Type.DELETE_EDGE);
          break;
        }
      case DELETE_EDGE:
        {
          result.setType(Mutation.Type.ADD_EDGE);
          break;
        }
      case CHANGE_TOKEN:
        {
          result.setTokenChange(revertTokenChangeMutation(result.getTokenChange()));
          break;
        }
      default:
        {
          break;
        }
    }
    return result.build();
  }

  /**
   * Returns a token mutation which undoes the changes caused by the passed token mutation. For
   * example, reverting an add token deletes the token.
   *
   * @param tokenMut the token mutation to reverse
   * @return the reverted token mutation
   */
  public static TokenMutation revertTokenChangeMutation(TokenMutation tokenMut) {
    TokenMutation.Builder result = TokenMutation.newBuilder(tokenMut);
    switch (tokenMut.getType()) {
      case ADD_TOKEN:
        {
          result.setType(TokenMutation.Type.DELETE_TOKEN);
          break;
        }
      case DELETE_TOKEN:
        {
          result.setType(TokenMutation.Type.ADD_TOKEN);
          break;
        }
      default:
        {
          break;
        }
    }
    return result.build();
  }
}
