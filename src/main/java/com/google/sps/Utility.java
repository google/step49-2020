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

import com.google.common.graph.*;
import com.google.gson.Gson;
import com.proto.GraphProtos.Node;
import com.google.common.graph.EndpointPair;
import java.util.List;
import java.util.ArrayList;
import com.google.protobuf.Struct;
import java.util.Set;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
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
   * @return a JSON object containing as entries the nodes and edges of this graph as well as the
   *     length of the list of mutations this graph is an intermediate result of applying
   */
  public static String graphToJson(MutableGraph<GraphNode> graph, int maxMutations) {
    Type typeOfNode = new TypeToken<Set<GraphNode>>() {}.getType();
    Type typeOfEdge = new TypeToken<Set<EndpointPair<GraphNode>>>() {}.getType();
    Gson gson = new Gson();
    String nodeJson = gson.toJson(graph.nodes(), typeOfNode);
    String edgeJson = gson.toJson(graph.edges(), typeOfEdge);
    String resultJson =
        new JSONObject()
            .put("nodes", nodeJson)
            .put("edges", edgeJson)
            .put("numMutations", maxMutations)
            .toString();
    return resultJson;
  }

  /**
   * @param original the original graph
   * @param curr the current (most recently-requested) graph
   * @param mutationNum number of mutations to apply
   * @param mutList mutation list
   * @return the resulting data graph or null if there was an error Requires that original != curr
   */
  public static DataGraph getGraphAtMutationNumber(
      DataGraph original, DataGraph curr, int mutationNum, List<Mutation> mutList) {
    boolean success = true;
    if (mutationNum > mutList.size() || mutationNum < 0) {
      return null;
    }

    if (curr.numMutations() <= mutationNum) { // going forward
      for (int i = curr.numMutations(); i < mutationNum; i++) {
        // Mutate graph operates in place
        success = curr.mutateGraph(mutList.get(i));
        if (!success) {
          return null;
        }
      }
      return DataGraph.create(curr.graph(), curr.graphNodesMap(), curr.roots(), mutationNum);
    } else {
      // Create a copy of the original graph and start from the original graph
      DataGraph originalCopy = original.getCopy();
      for (int i = 0; i < mutationNum; i++) {
        success = originalCopy.mutateGraph(mutList.get(i));
        if (!success) {
          return null;
        }
      }
      return DataGraph.create(
          originalCopy.graph(), originalCopy.graphNodesMap(), originalCopy.roots(), mutationNum);
    }
  }

  public static Mutation diffBetween(List<Mutation> mutList, int currIndex, int nextIndex) {
    if (Math.abs(currIndex - nextIndex) != 1) {
      return null;
    } else if (nextIndex - currIndex == 1) {
      return mutList.get(currIndex);
    } else {
      return invertMutation(mutList.get(nextIndex));
    }
  }

  private static Mutation invertMutation(Mutation mut) {
    Mutation.Builder invertedMut = Mutation.newBuilder(mut);
    Mutation.Type invertedMutType;
    switch (mut.getType()) {
      case ADD_NODE:
        invertedMutType = DELETE_NODE;
        break;
      case DELETE_NODE:
        invertedMutType = ADD_NODE;
        break;
      case ADD_EDGE:
        invertedMutType = DELETE_EDGE;
        break;
      case DELETE_EDGE:
        invertedMutType = ADD_EDGE;
        break;
      case CHANGE_TOKEN:
        invertedMutType = CHANGE_TOKEN;
        invertedMut.setTokenChange(invertTokenChange(mut.getTokenChange()));
        break;
      default:
        return null;
    }
    invertedMut.setType(invertedMutType).build();
    return invertedMut;
  }

  private static TokenMutation invertTokenChange(TokenMutation tokenMut) {
    TokenMutation.Builder invertedMut = TokenMutation.newBuilder(mut);
    if (tokenMut.getType() == TokenMutation.Type.ADD_TOKEN) {
      invertedMut.setType(TokenMutation.Type.DELETE_TOKEN);
    } else if (tokenMut.getType() == TokenMutation.Type.DELETE_TOKEN) {
      invertedMut.setType(TokenMutation.Type.ADD_TOKEN);
    } else {
      return null;
    }
    invertedMut.build();
  }
}
