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

  List<Integer> relevantMutationIndices = new ArrayList<>(); // should originally be everything
  List<Integer> defaultIndices = new ArrayList<>();

  int oldNumMutations = 0;
  String lastNodeName = "";

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
    System.out.println(mutationNumber);

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
      relevantMutationIndices = new ArrayList<>();
      for (int i = 0; i < mutList.size(); i++) {
        defaultIndices.add(i);
      }
      relevantMutationIndices = defaultIndices;
    }

    // Parameter for the nodeName the user searched for in the frontend
    String nodeNameParam = request.getParameter("nodeName");

    // The current graph at the specified index
    currDataGraph =
        Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, mutationNumber, mutList);

    // Current mutation number
    oldNumMutations = currDataGraph.numMutations(); // The old mutation number

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
      relevantMutationIndices = defaultIndices;
    } else {

      // Indicies of relevant mutations from the entire mutList
      relevantMutationIndices = Utility.getMutationIndicesOfNode(nodeNameParam, mutList);

      // TODO: find the index that's the next greatest on this list with binary search
      // That is, change the mutation num!!!
      if (!currDataGraph.graphNodesMap().containsKey(nodeNameParam)) {
        int newNumIndex = Utility.getNextGreatestNumIndex(relevantMutationIndices, oldNumMutations);
        int newNum = relevantMutationIndices.get(newNumIndex);
        if (newNum == -1) {
          // handle it
        }
        System.out.println(nodeNameParam);
        System.out.println(newNum);

        if (lastNodeName.equals(nodeNameParam)) {
          // searched for the same node at last time. don't want to truncate the mutList

        } else {
          relevantMutationIndices =
              relevantMutationIndices.subList(newNumIndex, relevantMutationIndices.size());
          relevantMutationIndices.add(0, oldNumMutations);
        }
        // Maybe make a copy instead of making this the currDataGraph
        currDataGraph =
            Utility.getGraphAtMutationNumber(originalDataGraph, currDataGraph, newNum, mutList);
        // Add null check?
        oldNumMutations = newNum;
        lastNodeName = nodeNameParam;
      } else {
        // Current graph has the node
        // So relevant indices are ok
      }

      // If the truncated graph is empty, it doesn't exist on the page. Check if there
      // are any
      // mutations that affect it
      truncatedMutList =
          Utility.getMutationsFromIndices(
              relevantMutationIndices, mutList); // only mutations relevant to the node

      // This is the single search
      truncatedGraph = currDataGraph.getReachableNodes(nodeNameParam, depthNumber);
      // truncatedGraph = currDataGraph.graph(); // TODO: change back to line above

      // If the graph is empty and there are no relevant mutations, then we give a
      // server error.
      if (truncatedGraph.nodes().isEmpty() && truncatedMutList.isEmpty()) {
        // If the truncated mutList is empty, then it is nowhere to be found!
        String error = "There are no nodes anywhere on this graph!";
        response.setHeader("serverError", error);
        return;
      }
    }

    String graphJson =
        Utility.graphToJson(truncatedGraph, truncatedMutList.size(), relevantMutationIndices);
    response.getWriter().println(graphJson);
  }
}
