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

package com.google.sps.servlets;

import com.google.common.graph.*;
import com.google.sps.data.DataGraph;
import com.google.sps.data.GraphNode;
import com.google.sps.data.Utility;
import com.proto.GraphProtos.Graph;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.MutationList;
import java.io.IOException;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  // GRAPH Data structures:
  // Create an undirected graph data structure to store the information, and
  // map each node name in the graph to the GraphNode objects. This is the graph &
  // map
  // we update with mutations
  // private MutableGraph<GraphNode> graph = null;
  // private HashMap<String, GraphNode> graphNodesMap = null;

  // private ImmutableGraph<GraphNode> graphOriginal = null; // never undergoes mutations

  // // Data structure that stores the roots of the graph across mutations
  // // Roots are nodes with no in-edges
  // private HashSet<String> roots = null;

  private List<Mutation> mutList = null;

  private DataGraph currDataGraph = null;
  private DataGraph originalDataGraph = null;

  /*
   * Called when a client submits a GET request to the /data URL
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    String depthParam = request.getParameter("depth");
    if (depthParam == null) {
      String error = "Improper depth parameter, cannot generate graph";
      response.setHeader("serverError", error);
      return;
    }

    int depthNumber = Integer.parseInt(depthParam);
    boolean success = true; // Innocent until proven guilty; successful until proven a failure

    // Initialize variables if any are null. Ideally should all be null or none
    // should be null
    if (currDataGraph == null || originalDataGraph == null) {

      Graph protoGraph =
          Graph.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/graph.txt"));
      // Originally both set to same data
      currDataGraph = new DataGraph(protoGraph);
      originalDataGraph = new DataGraph(protoGraph);
    }

    if (!success) {
      String error = "Failed to parse input graph into Guava graph - not a DAG!";
      response.setHeader("serverError", error);
      return;
    }

    // Mutations file hasn't been read yet
    if (mutList == null) {
      // Parse the contents of mutation.txt into a list of mutations
      mutList =
          MutationList.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/mutations.txt"))
              .getMutationList();
      // Only apply mutations once
      for (Mutation mut : mutList) {
        success =
            Utility.mutateGraph(
                mut,
                currDataGraph.getGraph(),
                currDataGraph.getGraphNodesMap(),
                currDataGraph.getRoots());
        if (!success) {
          String error = "Failed to apply mutation " + mut.toString() + " to graph";
          response.setHeader("serverError", error);
          return;
        }
      }
    }

    MutableGraph<GraphNode> truncatedGraph =
        Utility.getGraphWithMaxDepth(
            currDataGraph.getGraph(),
            currDataGraph.getRoots(),
            currDataGraph.getGraphNodesMap(),
            depthNumber);
    String graphJson = Utility.graphToJson(truncatedGraph, currDataGraph.getRoots());
    response.getWriter().println(graphJson);
  }
}
