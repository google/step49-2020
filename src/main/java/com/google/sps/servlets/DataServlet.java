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
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import proto.GraphProtos.Graph;
import proto.GraphProtos.Node;
import proto.MutationProtos.MutationList;
import proto.MutationProtos.Mutation;
import proto.MutationProtos.TokenMutation;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  /*
   * Called when a client submits a GET request to the /data URL Displays all
   * recorded user comments on page
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    /*
     * Code to read graph from graph.txt file in same directory and print it to the
     * console, read mutations from mutations.txt and print it to console
     */
    Graph inputGraph = Graph.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/graph.txt"));

    // Guava Graph
    MutableGraph<Node> graph = GraphBuilder.directed().build();

    List<String> roots = inputGraph.getRootNameList();\
    Map<String, Node> nodesMap = inputGraph.getNodesMapMap();
    for (String nodeName : nodesMap.keySet()) {
      
      Node thisNode = nodesMap.get(nodeName);
      // Guava graph
      graph.addNode(thisNode);

      // Add edges to the guava Graph
      for (String child : thisNode.getChildrenList()) {
        graph.putEdge(thisNode, nodesMap.get(child));
      }
    }
    System.out.println(graph.toString());

    List<Mutation> mutList = MutationList.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/mutations.txt"))
        .getMutationList();

    for (Mutation mut : mutList) {
      Node startNode = mut.getStartNode();
      Node endNode = mut.getEndNode();
      switch (mut.getType()) {
        case ADD_NODE:
        graph.addNode(startNode);
        for (String child : startNode.getChildrenList()) {
          graph.putEdge(startNode, nodesMap.get(child));
        }
          break;
        case ADD_EDGE:
        graph.putEdge(startNode, endNode);
          break;
        case DELETE_NODE:
        graph.removeNode(startNode);
          break;
        case DELETE_EDGE:
        graph.removeEdge(startNode, endNode);
          break;
        case CHANGE_TOKEN:
          changeNodeToken(graph, startNode, mut.getTokenChange());
          break;
        default:
          break;
      }
    }
  }

  private void changeNodeToken(MutableGraph<Node> graph, Node node, TokenMutation tokenMut) {
    String tokenName = tokenMut.getTokenName();
    // Add token
    if(tokenMut.getTypeValue() == 0) {
    }
  }

  /*
   * Called when a client submits a POST request to the /data URL Adds submitted
   * comment to internal record if the comment is non-empty.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }
}
