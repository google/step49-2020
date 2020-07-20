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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.io.InputStreamReader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.protobuf.TextFormat;
import com.proto.GraphProtos.Graph;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.MultiMutation;
import com.proto.MutationProtos.MutationList;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private DataGraph currDataGraph = null;
  private DataGraph originalDataGraph = null;
  private List<MultiMutation> mutList = null;

  // A list containing the indices of mutations that mutate the node we are
  // currently filtering by. It is initialized to all indices since we start
  // out not filtering by anything.
  List<Integer> filteredMutationIndices = new ArrayList<>();

  // A list containing all integers from 0 to mutList.size() - 1
  List<Integer> defaultIndices = new ArrayList<>();

  // A map from each node name to a list of indices in mutList where
  // that node is mutated
  HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();

  /*
   * Called when a client submits a GET request to the /data URL
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    boolean success = true;

    /*
     *********************************
     * Initialize Graph Variables
     *********************************
     */
    if (currDataGraph == null && originalDataGraph == null) {
      success = initializeGraphVariables(
        getServletContext().getResourceAsStream("/WEB-INF/initial_graph.textproto"));
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

    /*
     *************************************
     * Initialize Mutation List Variables
     *************************************
     */
    if (mutList == null) {
      initializeMutationVariables(getServletContext().getResourceAsStream("/WEB-INF/mutations.textproto"));
      // Populate the list of all possible mutation indices
      defaultIndices = IntStream.range(0, mutList.size() - 1).boxed().collect(Collectors.toList());
      // and initialize the current list of relevant indices to this because
      // we start out not filtering by anything
      filteredMutationIndices = defaultIndices;
    }

    /*
     *************************************
     * Read Request Parameters
     *************************************
     */

    String depthParam = request.getParameter("depth");
    String mutationParam = request.getParameter("mutationNum");
    String nodeNameParam = request.getParameter("nodeName");

    if (depthParam == null) {
      String error = "Improper depth parameter, cannot generate graph";
      response.setHeader("serverError", error);
      return;
    } else if (mutationParam == null) {
      String error = "Improper mutation number parameter, cannot generate graph";
      response.setHeader("serverError", error);
      return;
    } else if (nodeNameParam == null) {
      // We should not error out and should proceed as if not filtering in this
      // case
      nodeNameParam = "";
    }

    int depthNumber = Integer.parseInt(depthParam);
    int mutationNumber = Integer.parseInt(mutationParam);

    // Truncated version of graph to return to the client
    MutableGraph<GraphNode> truncatedGraph;

    // The list of mutations that need to be applied to the nodes in currDataGraph
    // to get the requested graph, filtered to only contain changes pertaining
    // to nodes in the truncated graph or the filtered node (null if the graph
    // requested is before the current graph in the sequence of mutations)
    MultiMutation diff = null;

    response.setContentType("application/json");
    String graphJson;

    /*
     *************************************************
     * Filtering Graph and Mutations By Searched Node
     *************************************************
     */

    if (nodeNameParam.length() == 0) {
      // No node has been searched for, so just return the requested graph 
      // upto the specified depth and reset any filtering of relevant indices

      // Compute the diff of changes to highlight if we're moving forward
      if (mutationNumber == currDataGraph.numMutations() + 1) {
        diff = Utility.getDiffBetween(mutList, mutationNumber);
      }

      // Try to generate the requested graph, catching and returning any error
      try {
        currDataGraph = Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, mutationNumber, mutList);
      } catch (IllegalArgumentException e) {
        String error = e.getMessage();
        response.setHeader("serverError", error);
        return;
      }
      // Truncate the generated graph to the required depth
      truncatedGraph = currDataGraph.getGraphWithMaxDepth(depthNumber);
      // Reset the list of indices that mutate the currently searched node
      // since there is no such node
      filteredMutationIndices = defaultIndices;
      graphJson = Utility.graphToJson(truncatedGraph, filteredMutationIndices, diff);
      response.getWriter().println(graphJson);
      return;
    }
    // A node is searched

    // First, get the indices at which this node is mutated, either by looking it
    // up in the cache or generating and caching them.
    if (!mutationIndicesMap.containsKey(nodeNameParam)) {
      mutationIndicesMap.put(nodeNameParam, Utility.getMutationIndicesOfNode(nodeNameParam, mutList));
    }
    filteredMutationIndices = mutationIndicesMap.get(nodeNameParam);


    // We case on whether the current graph contains the node or not
    if(currDataGraph.graphNodesMap().containsKey(nodeNameParam)) {
      // The graph displayed on screen does contain the given node

      // The new graph does not mutate the given node
      if(filteredMutationIndices.indexOf(mutationNumber) == -1) {
        String message = "The searched node exists, but is not mutated in this graph";
        response.setHeader("serverMessage", message);
      }
      if (mutationNumber > currDataGraph.numMutations()) {
        diff = Utility.getDiffBetween(mutList, mutationNumber);
      }
      // case 3: node is in the current graph. then relevant mutationIndices is ok
      currDataGraph = Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, mutationNumber, mutList);
      truncatedGraph = currDataGraph.getReachableNodes(nodeNameParam, depthNumber);
    } else {
      // The searched node is not in the graph
      if(filteredMutationIndices.size() == 0) {
        // and the searched node is never mutated
        String error = "The searched node does not exist";
        response.setHeader("serverError", error);
        return;
      } else {
        // TODO: what if the node is deleted at this point
        // the searched node is mutated at some future/previous point
        String message = "The searched node does not exist in this graph";
        response.setHeader("serverMessage", message);
        currDataGraph = Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, mutationNumber, mutList);
        truncatedGraph = GraphBuilder.directed().build();
      }
    }
    Set<String> truncatedGraphNodeNames = Utility.getNodeNamesInGraph(truncatedGraph);
    truncatedGraphNodeNames.add(nodeNameParam);
    diff = Utility.filterMultiMutationByNodes(diff, truncatedGraphNodeNames);
    graphJson = Utility.graphToJson(truncatedGraph, filteredMutationIndices, diff);
    response.getWriter().println(graphJson);


    //   // CASES:
    //   // 1. Node isn't on the current graph, node isn't in any mutations -> error (not
    //   // fatal)
    //   // 2. Node is not on the current graph, in a mutation though -> say it's not
    //   // here, jump to the mutation with it
    //   // this should only apply when a new node is searched
    //   // 3. Node is on the current graph -> then display current graph WITH the
    //   // relevant indices (no need to change indices)
    //   // Could either be the same node or a different node

    //   // Indicies of relevant mutations from the entire mutList
    //   // Add to map if doesn't exist yet
      

    //   // case 1: Node is not in the current graph or any graph
    //   if (!currDataGraph.graphNodesMap().containsKey(nodeNameParam) && filteredMutationIndices.isEmpty()) {
    //     String error = "There are no nodes anywhere on this graph!";
    //     response.setHeader("serverError", error);
    //     return;
    //   }
    //   // case 2: Node is not in the current graph
    //   if (!currDataGraph.graphNodesMap().containsKey(nodeNameParam)) {

    //     // index of the next element in relevantMutationsIndices that is greater than
    //     // currDataGraph.numMutations()
    //     int newNumIndex = Utility.getNextGreatestNumIndex(filteredMutationIndices, currDataGraph.numMutations());

    //     // shouldn't happen, but we're back to case 1.
    //     if (newNumIndex == -1) {
    //       String error = "There are no nodes anywhere on this graph!";
    //       response.setHeader("serverError", error);
    //       return;
    //     }

    //     // Give a warning but also move ahead to the next valid graph
    //     String message = "There are no nodes anywhere on this graph!";
    //     response.setHeader("serverMessage", message);

    //     // The index of the next mutation to look at in the ORIGINAL mutlist
    //     int newNum = filteredMutationIndices.get(newNumIndex);

    //     // only get the indices AFTER this one
    //     filteredMutationIndices = filteredMutationIndices.subList(newNumIndex, filteredMutationIndices.size());

    //     diff = Utility.getDiffBetween(mutList, newNum);

    //     // Update the current graph
    //     currDataGraph = Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, newNum, mutList);

    //     // This should not happen since
    //     if (currDataGraph == null) {
    //       String error = "Something went wrong when mutating the graph!";
    //       response.setHeader("serverError", error);
    //       return;
    //     }
    //     currIndex = 0;
    //   } else {
    //     if (mutationNumber > currDataGraph.numMutations()) {
    //       diff = Utility.getDiffBetween(mutList, mutationNumber);
    //     }
    //     // case 3: node is in the current graph. then relevant mutationIndices is ok
    //     currDataGraph = Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, mutationNumber, mutList);
    //     currIndex = filteredMutationIndices.indexOf(mutationNumber);
    //   }
    //   // This is the single search
    //   truncatedGraph = currDataGraph.getReachableNodes(nodeNameParam, depthNumber);
    //   Set<String> truncatedGraphNodeNames = Utility.getNodeNamesInGraph(truncatedGraph);
    //   truncatedGraphNodeNames.add(nodeNameParam);
    //   diff = Utility.filterMultiMutationByNodes(diff, truncatedGraphNodeNames);
    // }
  }

  private boolean initializeGraphVariables(InputStream graphInput) throws IOException {
    InputStreamReader graphReader = new InputStreamReader(graphInput);
    Graph.Builder graphBuilder = Graph.newBuilder();
    TextFormat.merge(graphReader, graphBuilder);
    Graph protoGraph = graphBuilder.build();

    Map<String, Node> protoNodesMap = protoGraph.getNodesMapMap();
    originalDataGraph = DataGraph.create();
    return originalDataGraph.graphFromProtoNodes(protoNodesMap);
  }

  private void initializeMutationVariables(InputStream mutationInput) throws IOException {
    InputStreamReader mutReader = new InputStreamReader(mutationInput);
    MutationList.Builder mutBuilder = MutationList.newBuilder();
    TextFormat.merge(mutReader, mutBuilder);
    mutList = mutBuilder.build().getMutationList();
  }
}
