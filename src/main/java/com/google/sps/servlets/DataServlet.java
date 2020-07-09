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
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.MutationList;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

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
    if (currDataGraph == null && originalDataGraph == null) {

      Graph protoGraph =
          Graph.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/graph.txt"));
      Map<String, Node> protoNodesMap = protoGraph.getNodesMapMap();
      // Originally both set to same data
      originalDataGraph = new DataGraph();
      success = originalDataGraph.graphFromProtoNodes(protoNodesMap);
      currDataGraph = new DataGraph();
      success = success && currDataGraph.graphFromProtoNodes(protoNodesMap);
    } else if (currDataGraph == null || originalDataGraph == null) {
      String error = "Invalid input";
      response.setHeader("serverError", error);
      return;
    }

    if (!success) {
      String error = "Failed to parse input graph into Guava graph - not a DAG!";
      response.setHeader("serverError", error);
      return;
    }

    MutableGraph<GraphNode> graph = currDataGraph.getGraph();
    HashMap<String, GraphNode> graphNodesMap = currDataGraph.getGraphNodesMap();
    HashSet<String> roots = currDataGraph.getRoots();

    // Mutations file hasn't been read yet
    if (mutList == null) {
      // Parse the contents of mutation.txt into a list of mutations
      mutList =
          MutationList.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/mutations.txt"))
              .getMutationList();
      // Only apply mutations once
      for (Mutation mut : mutList) {
        success = Utility.mutateGraph(mut, graph, graphNodesMap, roots);
        if (!success) {
          String error = "Failed to apply mutation " + mut.toString() + " to graph";
          response.setHeader("serverError", error);
          return;
        }
      }
    }

    MutableGraph<GraphNode> truncatedGraph =
        Utility.getGraphWithMaxDepth(graph, roots, graphNodesMap, depthNumber);
    String graphJson = Utility.graphToJson(truncatedGraph, currDataGraph.getRoots());
    response.getWriter().println(graphJson);
  }
}
