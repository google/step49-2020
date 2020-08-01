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

import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.appengine.repackaged.com.google.gson.JsonSyntaxException;
import com.google.common.collect.Sets;
import com.google.common.graph.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

import static com.google.sps.Utility.getMultiMutationAtIndex;
import static com.google.sps.Utility.getGraphAtMutationNumber;
import static com.google.sps.Utility.getNodeNamesInGraph;
import static com.google.sps.Utility.findRelevantMutations;
import static com.google.sps.Utility.getMutationIndicesOfToken;
import static com.google.sps.Utility.filterMultiMutationByNodes;
import static com.google.sps.Utility.graphToJson;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  // A data graph containing the information parsed from the input proto file
  private DataGraph originalDataGraph = null;
  // A data graph that represents the most recently requested graph
  private DataGraph currDataGraph = null;
  // A list of mutations to apply to the original data graph as parsed from the 
  // mutations proto file
  private List<MultiMutation> mutList = null;
  // We store a builder so that we can modify the contained mutation objects
  // to be non-redundant. For example, if we find that a mutation adds duplicate
  // tokens to a node we replace it with a trimmed version that doesn't add
  // duplicates. This is only possible if the mutation list is mutable.
  private MutationList.Builder mutListObj = null;
  // A list containing all integers from 0 to mutList.size() - 1
  List<Integer> defaultIndices = new ArrayList<>();
  // A map from each node name to a list of indices in mutList where
  // that node is mutated. In addition, the empty string is mapped
  // to the list [0, mutList.size() - 1].
  HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
  // A map from each encountered token name to a list of indices in mutList
  // where that token is mutated (either added or deleted from a node).
  HashMap<String, Set<Integer>> tokenIndicesMap = new HashMap<>();

  /*
   * Called when a client submits a GET request to the /data URL
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Used to detect whether various operations are performed successfully, and inform the 
    // user if not.
    boolean success = true;

    /*
     *********************************
     * Initialize Graph Variables
     *********************************
     */

    if (originalDataGraph == null && currDataGraph == null) {
      success =
          initializeGraphVariables(
              getServletContext().getResourceAsStream("/WEB-INF/initial_graph.textproto"));
      if (!success) {
        response.setHeader(
            "serverError", "Failed to parse input graph into Guava graph - not a DAG!");
        return;
      }
      // The most-recently requested graph always starts out as the initial graph
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
    if (mutListObj == null) {
      initializeMutationVariables(
          getServletContext().getResourceAsStream("/WEB-INF/mutations.textproto"));
      // Populate the list of all possible mutation indices
      defaultIndices = IntStream.range(0, mutList.size()).boxed().collect(Collectors.toList());
      // TODO: do we need this?
      // and store this as the list of relevant indices for filtering by empty string
      // (= not filtering)
      mutationIndicesMap.put("", defaultIndices);
    }

    /*
     *************************************
     * Read Request Parameters
     *************************************
     */
    // TODO: change depth to radius
    String radiusParam = request.getParameter("depth");
    String mutationNumParam = request.getParameter("mutationNum");
    String nodeNamesParam = request.getParameter("nodeNames");
    String tokenNameParam = request.getParameter("tokenName");

    if (radiusParam == null) {
      response.setHeader("serverError", "Improper radius parameter, cannot generate graph");
      return;
    } else if (mutationNumParam == null) {
      response.setHeader("serverError", "Improper mutation number parameter, cannot generate graph");
      return;
    }
    // If nodeNamesParam or tokenNameParam are null, we should just set them to empty and
    // not error out
    if (nodeNamesParam == null) {
      nodeNamesParam = "";
    }
    if (tokenNameParam == null) {
      tokenNameParam = "";
    }
    int radius = Integer.parseInt(radiusParam);
    int mutationNumber = Integer.parseInt(mutationNumParam);

    // Truncated version of graph to return to the client
    MutableGraph<GraphNode> truncatedGraph = GraphBuilder.directed().build();

    // A list containing the indices of mutations that any nodes displayed on 
    // screen as well as the searched nodes and any nodes containing the searched
    // token.
    List<Integer> filteredMutationIndices = new ArrayList<>();

    // The list of mutations that need to be applied to the nodes in currDataGraph
    // to get the requested graph (null if the graph requested is before the current
    // graph in the sequence of mutations)
    MultiMutation diff = null;
    // The above diff variable filtered to only contain changes relevant to nodes on-screen,
    // the filtered nodes and nodes containing the filtered token.
    MultiMutation filteredDiff = null;

    /*
     *************************************************************
     * Filtering Graph and Mutations By Searched Nodes and Tokens
     *************************************************************
     */

    // Get the diff if we are going forward in the list of mutations
    if (mutationNumber > currDataGraph.numMutations()) {
      diff = getMultiMutationAtIndex(mutList, mutationNumber);
    }

    // Stores the list of node names queried by the user
    List<String> nodeNames = new ArrayList<>();
    try {
      JsonArray nodeNameArr = JsonParser.parseString(nodeNamesParam).getAsJsonArray();
      for (int i = 0; i < nodeNameArr.size(); i++) {
        String curr = nodeNameArr.get(i).getAsString().trim();
        if (curr.length() > 0) {
          nodeNames.add(curr);
        }
      }
    } catch (JsonSyntaxException e) {
      response.setHeader("serverError", "The node names received do not form a valid JSON string");
    } catch (IllegalStateException e) {
      response.setHeader("serverError", "The node names received do not form a valid JSON array");
    }

    // A list of "roots" to return nodes at most "radius" distance from for the current graph
    // This differs from queriedNext below in that some queried nodes may be deleted or
    // cease to contain the queried token in the requested graph, so we include them in queried
    // just to show their mutations for a single step but exclude them from queried next so
    // their mutations (which are now irrelevant) are not shown.
    HashSet<String> queried = new HashSet<>();
    // A list of "roots" to return nodes at most "radius" distance from for the next graph
    HashSet<String> queriedNext = new HashSet<>();

    // We start by adding any queried node names
    // The reason we add them to queriedNext is to handle the case where a node in the list
    // doesn't exist now but is added in the future.
    if (nodeNames.size() > 0) {
      queried.addAll(nodeNames);
      queriedNext.addAll(nodeNames);
    }

    // We show mutations relevant to nodes that contain the token in the current graph
    if (currDataGraph.tokenMap().containsKey(tokenNameParam)) {
      queried.addAll(currDataGraph.tokenMap().get(tokenNameParam));
    }

    // Get the graph at the requested mutation number
    try {
      currDataGraph =
          getGraphAtMutationNumber(originalDataGraph, currDataGraph, mutationNumber, mutListObj);
    } catch (IllegalArgumentException e) {
      response.setHeader("serverError", e.getMessage());
      return;
    }

    // We also show mutations relevant to nodes that contain the token in the new graph
    // Mutations relevant to these nodes should be shown
    if (currDataGraph.tokenMap().containsKey(tokenNameParam)) {
      queried.addAll(currDataGraph.tokenMap().get(tokenNameParam));
      queriedNext.addAll(currDataGraph.tokenMap().get(tokenNameParam));
    }
    // This condition exists to prevent entry into this case when the user
    // searches for a non-existent token and no node. In this case, queried
    // is empty so using the below logic will return the whole graph. To avoid
    // this, we initialize truncatedGraph to the empty graph and include this
    // condition
    if (tokenNameParam.length() == 0 || queried.size() != 0) {
      // Truncate the graph from the nodes that the client had searched for
      truncatedGraph = currDataGraph.getReachableNodes(queried, radius);
    }

    // The next graph to display to the client
    MutableGraph<GraphNode> truncatedGraphNext;
    // Empty queriedNext just gives an empty graph
    if (queriedNext.isEmpty()) {
      truncatedGraphNext = GraphBuilder.undirected().build();
    } else {
      // If queried and queriedNext contain the same nodes, then there is no reason 
      // to regenerate the graph
      truncatedGraphNext =
          queried.equals(queriedNext)
              ? truncatedGraph
              : currDataGraph.getReachableNodes(queriedNext, radius);
    }

    // If we are not filtering the graph or limiting its depth, show all mutations of all nodes
    if (nodeNames.size() == 0
        && tokenNameParam.length() == 0
        && truncatedGraph.equals(currDataGraph.graph())) {
      filteredMutationIndices = defaultIndices;
      filteredDiff = diff;
    } else {
      Set<String> truncatedGraphNodeNames = getNodeNamesInGraph(truncatedGraph);
      Set<String> truncatedGraphNodeNamesNext = getNodeNamesInGraph(truncatedGraphNext);

      // A set containing a indices where nodes currently displayed on the graph
      // or queried are mutated
      Set<Integer> mutationIndicesSet = new HashSet<>();

      // Add all mutations relevant to on-screen nodes
      mutationIndicesSet.addAll(
          findRelevantMutations(truncatedGraphNodeNamesNext, mutationIndicesMap, mutList));

      // Add all mutations relevant to the queried token, computing and caching it if it 
      // hasn't been done already
      if (!tokenIndicesMap.containsKey(tokenNameParam)) {
        tokenIndicesMap.put(tokenNameParam, getMutationIndicesOfToken(tokenNameParam, mutList));
      }
      mutationIndicesSet.addAll(tokenIndicesMap.get(tokenNameParam));
      
      // Add all mutations relevant to the queried node names
      mutationIndicesSet.addAll(findRelevantMutations(nodeNames, mutationIndicesMap, mutList));
      filteredMutationIndices = new ArrayList<>(mutationIndicesSet);
      Collections.sort(filteredMutationIndices);

      // Show mutations relevant to nodes that are related to on-screen nodes, nodes that
      // used to/still have the queried token and any queried nodes
      filteredDiff = filterMultiMutationByNodes(diff, Sets.union(truncatedGraphNodeNames, queried));
    }

    /*
     ***********************
     * Error Handling
     ***********************
     */

    // We set the headers in the following 4 scenarios:
    // truncatedGraph.nodes().size() == 0 means something was queried but wasn't found
    // in the graph
    // filteredMutationIndices.size() == 0 means the searched object is never mutated
    if (truncatedGraph.nodes().size() == 0 && filteredMutationIndices.size() == 0) {
      response.setHeader(
          "serverError",
          "The searched node/token does not exist anywhere in this graph or in mutations");
      return;
    }
    // truncatedGraph.nodes().size() == 0 means something was queried but wasn't found
    // in the graph
    // filteredMutationIndices.size() != 0 means the searched object is mutated at some 
    // point
    // filteredDiff == null || filteredDiff.getMutationList().size() == 0 means that none
    // of the searched objects are mutated here. This exists to prevent this message from 
    // being emitted when the searched object is deleted in this graph so it doesn't exist 
    // but is still mutated
    if (truncatedGraph.nodes().size() == 0
        && filteredMutationIndices.size() != 0
        && (filteredDiff == null || filteredDiff.getMutationList().size() == 0)) {
      response.setHeader(
          "serverMessage",
          "The searched node/token does not exist in this graph, so nothing is shown. However, it"
              + " is mutated at some other step. Please click next or previous to navigate to a"
              + " graph where this node exists.");
    }
    // truncatedGraph.nodes().size() != 0 means the queried object was found in this graph
    // !(mutationNumber == -1 && nodeNames.size() == 0 && tokenNameParam.length() == 0) 
    // is included to avoid emitting this message when we are on the initial graph with
    // no node names or tokens searched because -1 is never a valid mutation index
    // filteredMutationIndices.indexOf(mutationNumber) == -1 means that the searched object
    // is not mutated in this graph
    if (truncatedGraph.nodes().size() != 0
        && !(mutationNumber == -1 && nodeNames.size() == 0 && tokenNameParam.length() == 0)
        && filteredMutationIndices.indexOf(mutationNumber) == -1) {
      response.setHeader(
          "serverMessage",
          "The searched node/token exists in this graph. However, it is not mutated in this"
              + " graph. Please click next or previous if you wish to see where it was"
              + " mutated!");
    }

    // filteredMutationIndices.indexOf(mutationNumber) != -1 means some queried objects were
    // mutated in this graph
    // filteredDiff != null ensures that this message only shows when there is an actual diff
    // filteredDiff.getMutationList().size() == 0 means that none of the mutations in the diff
    // pertained solely to on-screen nodes
    if (filteredMutationIndices.indexOf(mutationNumber) != -1
        && filteredDiff != null
        && filteredDiff.getMutationList().size() == 0) {
      response.setHeader(
          "serverMessage",
          "The desired set of nodes is mutated in this graph but your other parameters (for eg."
              + " radius), limit the display of the mutations. Please try increasing your radius"
              + " to view the mutation.");
    }

    /*
     ***********************
     * Sending Response
     ***********************
     */

    response.setContentType("application/json");
    String graphJson =
        graphToJson(
            truncatedGraph, filteredMutationIndices, filteredDiff, mutList.size(), queriedNext);
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
   * Private function to intialize the mutation list.
   *
   * @param mutationInput InputStream to initialize mutation list variable over
   * @throws IOException if something goes wrong during the reading
   */
  private void initializeMutationVariables(InputStream mutationInput) throws IOException {
    InputStreamReader mutReader = new InputStreamReader(mutationInput);
    mutListObj = MutationList.newBuilder();
    TextFormat.merge(mutReader, mutListObj);
    mutList = mutListObj.getMutationList();
  }
}
