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
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.graph.MutableGraph;
import com.proto.GraphProtos.Graph;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.MutationList;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private List<Mutation> mutList = null;

  private DataGraph currDataGraph = null;
  private DataGraph originalDataGraph = null;

  private String latestSearchNode = null;

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
      // InputStreamReader graphReader = new
      // InputStreamReader(getServletContext().getResourceAsStream("/WEB-INF/initial_graph.textproto"));
      // Graph.Builder graphBuilder = Graph.newBuilder();
      // TextFormat.merge(graphReader, graphBuilder);
      // Graph protoGraph = graphBuilder.build();

      /*
       * This code is used to read a graph specified in proto binary format.
       */
      Graph protoGraph =
          Graph.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/graph.txt"));
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
    // Relevant mutation indicies, start as everything
    List<Integer> relevantMutationIndices; // should originally be everything
    // Mutations file hasn't been read yet
    if (mutList == null) {
      /*
       * The below code is used to read a mutation list specified in textproto form
       */
      // InputStreamReader mutReader = new
      // InputStreamReader(getServletContext().getResourceAsStream("/WEB-INF/mutations.textproto"));
      // MutationList.Builder mutBuilder = MutationList.newBuilder();
      // TextFormat.merge(mutReader, mutBuilder);
      // mutList = mutBuilder.build().getMutationList();
      /*
       * This code is used to read a mutation list specified in proto binary format.
       */
      // Parse the contents of mutation.txt into a list of mutations
      mutList =
          MutationList.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/mutations.txt"))
              .getMutationList();
    }

    // Parameter for the nodeName the user searched for in the frontend
    String nodeNameParam = request.getParameter("nodeName");

    // The current graph at the specified index
    currDataGraph =
        Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, mutationNumber, mutList);

    // Current mutation number
    int oldNumMutations = currDataGraph.numMutations(); // The old mutation number

    // returns null if either mutation isn't able to be applied or if num < 0
    if (currDataGraph == null) {
      String error = "Failed to apply mutations!";
      response.setHeader("serverError", error);
      return;
    }

    List<Mutation> truncatedMutList;

    MutableGraph<GraphNode> truncatedGraph;
    // If a node is searched, get the graph with just the node. Otherwise, use the
    // whole graph

    // No query
    if (nodeNameParam == null || nodeNameParam.length() == 0) {
      truncatedGraph = currDataGraph.getGraphWithMaxDepth(depthNumber);
      truncatedMutList = mutList;
    } else {

      // Indicies of relevant mutations
      relevantMutationIndices = Utility.getMutationIndicesOfNode(nodeNameParam, mutList);

      // TODO: find the index that's the next greatest on this list with binary search
      // That is, change the mutation num!!!
      int newNum = Utility.getNextGreatestNum(relevantMutationIndices, oldNumMutations);

      // Maybe make a copy instead of making this the currDataGraph
     
      DataGraph tempData = Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, newNum, mutList);

      // If the truncated graph is empty, it doesn't exist on the page. Check if there
      // are any
      // mutations that affect it
      truncatedMutList =
          Utility.getMutationsFromIndices(
              relevantMutationIndices, mutList); // only mutations relevant to the node

      // This is the single search
      truncatedGraph = tempData.getReachableNodes(nodeNameParam, depthNumber);

      // oldNumMutations is the number of mutations that were applied.
      // you want to see where this falls in the new one

      // If the graph is empty and there are no relevant mutations, then we give a
      // server error.
      if (truncatedGraph.nodes().isEmpty() && truncatedMutList.isEmpty()) {
        // If the truncated mutList is empty, then it is nowhere to be found!
        String error = "There are no nodes anywhere on this graph!";
        response.setHeader("serverError", error);
        return;
      }
    }

    String graphJson = Utility.graphToJson(truncatedGraph, truncatedMutList.size());
    response.getWriter().println(graphJson);
  }
}
