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

import java.io.IOException;
import java.util.ArrayList;

import java.io.InputStreamReader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.graph.MutableGraph;
import com.google.protobuf.TextFormat;
import com.proto.GraphProtos.Graph;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.MultiMutation;
import com.proto.MutationProtos.MutationList;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private List<MultiMutation> mutList = null;

  private DataGraph currDataGraph = null;
  private DataGraph originalDataGraph = null;

  // Starts out as all indices since we are not filtering by anything
  List<Integer> filteredMutationIndices = new ArrayList<>();
  List<Integer> defaultIndices = new ArrayList<>();

  // TODO: figure out if we should generate this when we read in the mutList
  HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();

  /*
   * Called when a client submits a GET request to the /data URL
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String depthParam = request.getParameter("depth");
    String mutationParam = request.getParameter("mutationNum");
    if (depthParam == null) {
      String error = "Improper depth parameter, cannot generate graph";
      response.setHeader("serverError", error);
      return;
    } else if (mutationParam == null) {
      String error = "Improper mutation number parameter, cannot generate graph";
      response.setHeader("serverError", error);
      return;
    }

    int depthNumber = Integer.parseInt(depthParam);
    int mutationNumber = Integer.parseInt(mutationParam);

    boolean success = true; // Innocent until proven guilty; successful until proven a failure

    // Initialize variables if any are null. Ideally should all be null or none
    // should be null
    if (currDataGraph == null && originalDataGraph == null) {

      /*
       * The below code is used to read a graph specified in textproto form
       */
      InputStreamReader graphReader =
          new InputStreamReader(
              getServletContext().getResourceAsStream("/WEB-INF/initial_graph.textproto"));
      Graph.Builder graphBuilder = Graph.newBuilder();
      TextFormat.merge(graphReader, graphBuilder);
      Graph protoGraph = graphBuilder.build();

      Map<String, Node> protoNodesMap = protoGraph.getNodesMapMap();
      // Originally both set to same data
      originalDataGraph = DataGraph.create();
      success = originalDataGraph.graphFromProtoNodes(protoNodesMap);
      String error = "Failed to parse input graph into Guava graph - not a DAG!";
      if (!success) {
        response.setHeader("serverError", error);
        return;
      }
      currDataGraph = originalDataGraph.getCopy();
    } else if (currDataGraph == null || originalDataGraph == null) {
      String error = "Invalid input";
      response.setHeader("serverError", error);
      return;
    }

    // Mutations file hasn't been read yet
    if (mutList == null) {
      /*
       * The below code is used to read a mutation list specified in textproto form
       */
      InputStreamReader mutReader =
          new InputStreamReader(
              getServletContext().getResourceAsStream("/WEB-INF/mutations.textproto"));
      MutationList.Builder mutBuilder = MutationList.newBuilder();
      TextFormat.merge(mutReader, mutBuilder);
      mutList = mutBuilder.build().getMutationList();

      // initially, all mutation indices are relevant so we put that into the default
      // and set them equal.
      for (int i = 0; i < mutList.size(); i++) {
        defaultIndices.add(i);
      }
      // Relevant mutation indicies start as everything
      filteredMutationIndices = defaultIndices;
    }

    // Parameter for the nodeName the user searched for in the frontend
    String nodeNameParam = request.getParameter("nodeName");

    // Truncated version of the graph and mutation list, for returning to the client
    MutableGraph<GraphNode> truncatedGraph;

    MultiMutation diff = null;

    int currIndex = 0;

    // No node is searched, so use the whole graph
    if (nodeNameParam == null || nodeNameParam.length() == 0) {
      // Just get the specified deptg, the mutation list, and relevant mutations as
      // they are

      if (mutationNumber == currDataGraph.numMutations() + 1) {
        diff = Utility.getDiffBetween(mutList, mutationNumber);
      }
      try {
        currDataGraph =
            Utility.getGraphAtMutationNumber(
                originalDataGraph, currDataGraph, mutationNumber, mutList);
      } catch (IllegalArgumentException e) {
        String error = e.getMessage();
        response.setHeader("serverError", error);
        return;
      }
      truncatedGraph = currDataGraph.getGraphWithMaxDepth(depthNumber);
      filteredMutationIndices = defaultIndices;
      currIndex = mutationNumber;
    } else { // A node is searched

      // CASES:
      // 1. Node isn't on the current graph, node isn't in any mutations -> error (not
      // fatal)
      // 2. Node is not on the current graph, in a mutation though -> say it's not
      // here, jump to the mutation with it
      // this should only apply when a new node is searched
      // 3. Node is on the current graph -> then display current graph WITH the
      // relevant indices (no need to change indices)
      // Could either be the same node or a different node

      // Indicies of relevant mutations from the entire mutList
      // Add to map if doesn't exist yet
      if (!mutationIndicesMap.containsKey(nodeNameParam)) {
        mutationIndicesMap.put(
            nodeNameParam, Utility.getMutationIndicesOfNode(nodeNameParam, mutList));
      }
      filteredMutationIndices = mutationIndicesMap.get(nodeNameParam);

      // case 1: Node is not in the current graph or any graph
      if (!currDataGraph.graphNodesMap().containsKey(nodeNameParam)
          && filteredMutationIndices.isEmpty()) {
        String error = "There are no nodes anywhere on this graph!";
        response.setHeader("serverError", error);
        return;
      }
      // case 2: Node is not in the current graph
      if (!currDataGraph.graphNodesMap().containsKey(nodeNameParam)) {

        // index of the next element in relevantMutationsIndices that is greater than
        // currDataGraph.numMutations()
        int newNumIndex =
            Utility.getNextGreatestNumIndex(filteredMutationIndices, currDataGraph.numMutations());

        // shouldn't happen, but we're back to case 1.
        if (newNumIndex == -1) {
          String error = "There are no nodes anywhere on this graph!";
          response.setHeader("serverError", error);
          return;
        }

        // Give a warning but also move ahead to the next valid graph
        String message = "There are no nodes anywhere on this graph!";
        response.setHeader("serverMessage", message);

        // The index of the next mutation to look at in the ORIGINAL mutlist
        int newNum = filteredMutationIndices.get(newNumIndex);

        // only get the indices AFTER this one
        filteredMutationIndices =
            filteredMutationIndices.subList(newNumIndex, filteredMutationIndices.size());

        diff = Utility.getDiffBetween(mutList, newNum);

        // Update the current graph
        currDataGraph =
            Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, newNum, mutList);

        // This should not happen since
        if (currDataGraph == null) {
          String error = "Something went wrong when mutating the graph!";
          response.setHeader("serverError", error);
          return;
        }
        currIndex = 0;
      } else {
        if (mutationNumber > currDataGraph.numMutations()) {
          diff = Utility.getDiffBetween(mutList, mutationNumber);
        }
        // case 3: node is in the current graph. then relevant mutationIndices is ok
        currDataGraph =
            Utility.getGraphAtMutationNumber(
                originalDataGraph, currDataGraph, mutationNumber, mutList);
        currIndex = filteredMutationIndices.indexOf(mutationNumber);
      }
      // This is the single search
      truncatedGraph = currDataGraph.getReachableNodes(nodeNameParam, depthNumber);
      Set<String> truncatedGraphNodeNames = Utility.getNodeNamesInGraph(truncatedGraph);
      truncatedGraphNodeNames.add(nodeNameParam);
      diff = Utility.filterMultiMutationByNodes(diff, truncatedGraphNodeNames);
    }
    
    String graphJson =
        Utility.graphToJson(truncatedGraph, filteredMutationIndices, diff, currIndex);

    response.getWriter().println(graphJson);
  }
}
