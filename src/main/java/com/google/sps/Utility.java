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
import com.google.protobuf.Struct;
import com.google.common.base.Preconditions;
import java.util.Set;

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
      MutableGraph<GraphNode> graph, int maxMutations, MultiMutation mutDiff) {
    Type typeOfNode = new TypeToken<Set<GraphNode>>() {}.getType();
    Type typeOfEdge = new TypeToken<Set<EndpointPair<GraphNode>>>() {}.getType();
    Gson gson = new Gson();
    String nodeJson = gson.toJson(graph.nodes(), typeOfNode);
    String edgeJson = gson.toJson(graph.edges(), typeOfEdge);
    String mutDiffJson =
        (mutDiff == null || !mutDiff.isInitialized()) ? "" : gson.toJson(mutDiff.getMutationList());
    String reason = (mutDiff == null || !mutDiff.isInitialized()) ? "" : mutDiff.getReason();
    String resultJson =
        new JSONObject()
            .put("nodes", nodeJson)
            .put("edges", edgeJson)
            .put("numMutations", maxMutations)
            .put("mutationDiff", mutDiffJson)
            .put("reason", reason)
            .toString();
    return resultJson;
  }

  /**
   * @param original the original graph
   * @param curr the current (most recently-requested) graph (requires that original != curr)
   * @param mutationNum number of mutations to apply
   * @param mutList mutation list
   * @return the resulting data graph or null if there was an error
   */
  public static List<Object> getGraphAtMutationNumber(
      DataGraph original, DataGraph curr, int mutationNum, List<MultiMutation> mutList)
      throws IllegalArgumentException {
    Preconditions.checkArgument(
        original != curr, "The current graph and the original graph refer to the same object");

    List<Object> ret = new ArrayList<>();
    String error = "";
    if (mutationNum > mutList.size() || mutationNum < 0) {
      return null;
    }

    if (curr.numMutations() <= mutationNum) { // going forward
      for (int i = curr.numMutations(); i < mutationNum; i++) {
        // Mutate graph operates in place
        MultiMutation multiMut = mutList.get(i);
        List<Mutation> mutations = multiMut.getMutationList();
        for (Mutation mut : mutations) {
          String thisError = curr.mutateGraph(mut);
          if (thisError.length() != 0) {
            error += thisError;
          }
        }
      }
      ret.add(DataGraph.create(curr.graph(), curr.graphNodesMap(), curr.roots(), mutationNum));
      if (error.length() != 0) {
        ret.add(error);
      }
    } else {
      // Create a copy of the original graph and start from the original graph
      DataGraph originalCopy = original.getCopy();
      for (int i = 0; i < mutationNum; i++) {
        MultiMutation multiMut = mutList.get(i);
        List<Mutation> mutations = multiMut.getMutationList();
        for (Mutation mut : mutations) {
          String thisError = originalCopy.mutateGraph(mut);
          if (thisError.length() != 0) {
            error += thisError;
          }
        }
      }
      ret.add(
          DataGraph.create(
              originalCopy.graph(),
              originalCopy.graphNodesMap(),
              originalCopy.roots(),
              mutationNum));
      if (error.length() != 0) {
        ret.add(error);
      }
    }
    return ret;
  }

  /**
   * Returns a multi-mutation (list of mutations) that need to be applied to get from the graph at
   * currIndex to the graph at nextIndex as long as nextIndex = currIndex + 1
   *
   * @param multiMutList the list of multi-mutations that are to be applied to the initial graph
   * @param currIndex the index in the above list the current graph is at
   * @param nextIndex the next index to generate a graph for
   * @return a multimutation with all the changes to apply to the current graph to get the next
   *     graph
   */
  public static MultiMutation diffBetween(
      List<MultiMutation> multiMutList, int currIndex, int nextIndex) {
    if (currIndex < 0
        || currIndex > multiMutList.size()
        || nextIndex < 0
        || nextIndex > multiMutList.size()) {
      // Out of bounds indices
      return null;
    }
    if (nextIndex - currIndex == 1) {
      // Non-adjacent indices
      return multiMutList.get(currIndex);
    }
    return null;
  }
}
