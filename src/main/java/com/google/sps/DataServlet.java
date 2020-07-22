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
  // that node is mutated. In addition, the empty string is mapped
  // to the list [0, mutList.size() - 1].
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
      success =
          initializeGraphVariables(
              getServletContext().getResourceAsStream("/WEB-INF/initial_graph.textproto"));
      if (!success) {
        response.setHeader(
            "serverError", "Failed to parse input graph into Guava graph - not a DAG!");
        return;
      }
      currDataGraph = originalDataGraph.getCopy();
    } else if (currDataGraph == null || originalDataGraph == null) {
      response.setHeader("serverError", "Invalid input");
      return;
    }

    /*
     *************************************
     * Initialize Mutation List Variables
     *************************************
     */
    if (mutList == null) {
      initializeMutationVariables(
          getServletContext().getResourceAsStream("/WEB-INF/mutations.textproto"));
      // Populate the list of all possible mutation indices
      defaultIndices = IntStream.range(0, mutList.size() - 1).boxed().collect(Collectors.toList());
      // and store this as the list of relevant indices for filtering by empty string
      // (= not
      // filtering)
      mutationIndicesMap.put("", defaultIndices);
    }

    /*
     *************************************
     * Read Request Parameters
     *************************************
     */

    String depthParam = request.getParameter("depth");
    String mutationParam = request.getParameter("mutationNum");
    String nodeNameParam = request.getParameter("nodeName");
    String tokenParam = request.getParameter("tokenName");

    if (depthParam == null) {
      String error = "Improper depth parameter, cannot generate graph";
      response.setHeader("serverError", error);
      return;
    } else if (mutationParam == null) {
      String error = "Improper mutation number parameter, cannot generate graph";
      response.setHeader("serverError", error);
      return;
    }
    // If nodeNameParam or tokenParam are null, we should just set them to empty and
    // not error out
    if (nodeNameParam == null) {
      nodeNameParam = "";
    }
    if (tokenParam == null) {
      tokenParam = "";
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

    // Get the diff if we are going forward in the list of mutations
    if (mutationNumber > currDataGraph.numMutations()) {
      diff = Utility.getMultiMutationAtIndex(mutList, mutationNumber);
    }
    // Find the indices that mutate the searched node, computing and caching them
    // if this has not been done already
    if (!mutationIndicesMap.containsKey(nodeNameParam)) {
      mutationIndicesMap.put(
          nodeNameParam, Utility.getMutationIndicesOfNode(nodeNameParam, mutList));
    }

    List<String> queried = new ArrayList<>();
    List<List<Integer>> allRelevantMutationIndices = new ArrayList<>();

    // Possible combinations of nodeName and token:
    // 1. Both empty -> can just look at nodeNameParam
    // 2. nodeName nonempty, token empty -> just look at nodeNameParam
    // 3. nodeName empty, token nonempty -> look only at token
    // 4. Both nondempty -> look at both
    // So we look at nodeName param in the case that both are empty and token is
    // empty

    // nodeNameParam is relevant in these case. otherwise this means tokenParam is
    // not empty but nodeName is, so we should not consider the nodeNameParam.
    // statement is shortened from tokenParam.length() == 0 || (tokenParam.length()
    // != 0 &&
    // nodeNameParam.length() != 0)
    if (tokenParam.length() == 0 || nodeNameParam.length() != 0) {
      queried.add(nodeNameParam);
      allRelevantMutationIndices.add(mutationIndicesMap.get(nodeNameParam));
    }
    allRelevantMutationIndices.add(Utility.getMutationIndicesOfToken(tokenParam, mutList));

    // Process the tokens here - if tokens are empty it won't be in the map, this
    // won't happen
    // If the token is contained, then get the nodes associated with the token and
    // add them to the queried nodes
    if (currDataGraph.tokenMap().containsKey(tokenParam)) {
      queried.addAll(currDataGraph.tokenMap().get(tokenParam));
      for (String s : currDataGraph.tokenMap().get(tokenParam)) {
        if (!mutationIndicesMap.containsKey(s)) {
          mutationIndicesMap.put(s, Utility.getMutationIndicesOfNode(s, mutList));
        }
        allRelevantMutationIndices.add(mutationIndicesMap.get(s));
      }
    }

    // Get a sorted indice list with everything
    filteredMutationIndices = Utility.mergeSortedLists(allRelevantMutationIndices);
    // Get the graph at the requested mutation number and truncate it
    currDataGraph =
        Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, mutationNumber, mutList);

    truncatedGraph = currDataGraph.getReachableNodes(queried, depthNumber);

    // We set the headers in the following 3 scenarios:
    // The searched node is not in the graph and is never mutated
    if (truncatedGraph.nodes().size() == 0 && filteredMutationIndices.size() == 0) {
      response.setHeader(
          "serverError", "The searched node does not exist anywhere in this graph or in mutations");
      return;
    }
    // The searched node is not in the graph but is mutated at some past/future point
    if (truncatedGraph.nodes().size() == 0 && filteredMutationIndices.size() != 0) {
      response.setHeader(
          "serverMessage",
          "The searched node does not exist in this graph, but is mutated at some point");
    }
    // The searched node exists but is not mutated in the current graph
    if (truncatedGraph.nodes().size() != 0
        && mutationNumber != -1
        && filteredMutationIndices.indexOf(mutationNumber) == -1) {
      response.setHeader(
          "serverMessage", "The searched node exists, but is not mutated in this graph");
    }

    // We filter the multimutation if there was a node searched
    if (nodeNameParam.length() != 0) {
      Set<String> truncatedGraphNodeNames = Utility.getNodeNamesInGraph(truncatedGraph);
      truncatedGraphNodeNames.add(nodeNameParam);
      diff = Utility.filterMultiMutationByNodes(diff, truncatedGraphNodeNames);
    }
    graphJson = Utility.graphToJson(truncatedGraph, filteredMutationIndices, diff);
    response.getWriter().println(graphJson);
  }

  /**
   * Private function to intitialize graph variables. Returns a boolean to represent whether the
   * InpuStream was read successfully.
   *
   * @param graphInput InputStream to initialize graph variables over
   * @return whether variables were initialized properly; true if successful and false otherwise
   * @throws IOException if something goes wrong during the reading
   */
  private boolean initializeGraphVariables(InputStream graphInput) throws IOException {
    InputStreamReader graphReader = new InputStreamReader(graphInput);
    Graph.Builder graphBuilder = Graph.newBuilder();
    TextFormat.merge(graphReader, graphBuilder);
    Graph protoGraph = graphBuilder.build();

    Map<String, Node> protoNodesMap = protoGraph.getNodesMapMap();
    originalDataGraph = DataGraph.create();
    return originalDataGraph.graphFromProtoNodes(protoNodesMap);
  }

  /**
   * Private function to intialize the mutation list. Returns a boolean to represent whether the
   * InputStream was read successfully.
   *
   * @param mutationInput InputStream to initialize variable over
   * @return whether variables were initialized properly; true if successful and false otherwise
   * @throws IOException if something goes wrong during the reading
   */
  private void initializeMutationVariables(InputStream mutationInput) throws IOException {
    InputStreamReader mutReader = new InputStreamReader(mutationInput);
    MutationList.Builder mutBuilder = MutationList.newBuilder();
    TextFormat.merge(mutReader, mutBuilder);
    mutList = mutBuilder.build().getMutationList();
  }
}
