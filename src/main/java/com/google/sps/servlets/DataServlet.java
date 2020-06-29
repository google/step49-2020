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
    Graph inputGraph = Graph.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/inputGraph.txt"));

    // Guava Graph
    MutableGraph<Node> graph = GraphBuilder.directed().build();

    List<String> roots = inputGraph.getRootNameList();
    System.out.println("Roots " + roots.toString());
    Map<String, Node> nodesMap = inputGraph.getNodesMapMap();
    for (String nodeName : nodesMap.keySet()) {
      
      // Guava graph
      graph.addNode(nodesMap.get(nodeName));
      System.out.print("Node " + nodeName);
      Node thisNode = nodesMap.get(nodeName);

      // Add edges to the guava Graph
      for (String child : thisNode.getChildrenList()) {
        graph.putEdge(thisNode, nodesMap.get(child));
      }
    }
    System.out.println(graph.toString());

    List<Mutation> mutList = MutationList.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/mutations.txt"))
        .getMutationList();
    for (Mutation m : mutList) {
      System.out.println("Type " + m.getType());
      System.out.println("Start Node " + m.getStartNode());
      System.out.println("End Node " + m.getEndNode());
    }
    /*
     * Code to build graph and write it to graph.txt file in target directory and
     * build graph and write it to mutations.txt in the target directory
     */
    // Node.Builder nodeA = Node.newBuilder();
    // nodeA.setName("A");
    // nodeA.addChildren("B");
    // nodeA.addChildren("C");

    // Node.Builder nodeB = Node.newBuilder();
    // nodeB.setName("B");
    // nodeB.addChildren("D");

    // Node.Builder nodeC = Node.newBuilder();
    // nodeC.setName("C");

    // Node.Builder nodeD = Node.newBuilder();
    // nodeD.setName("D");

    // Node.Builder nodeE = Node.newBuilder();
    // nodeE.setName("E");
    // nodeE.addChildren("F");

    // Node.Builder nodeF = Node.newBuilder();
    // nodeF.setName("F");

    // Node.Builder nodeG = Node.newBuilder();
    // nodeG.setName("G");

    // Node.Builder nodeH = Node.newBuilder();
    // nodeH.setName("H");

    // Graph.Builder graph = Graph.newBuilder();
    // graph.addRootName("A");
    // graph.addRootName("E");
    // graph.putNodesMap("A", nodeA.build());
    // graph.putNodesMap("B", nodeB.build());
    // graph.putNodesMap("C", nodeC.build());
    // graph.putNodesMap("D", nodeD.build());
    // graph.putNodesMap("E", nodeE.build());
    // graph.putNodesMap("F", nodeF.build());

    // Mutation.Builder mutationA = Mutation.newBuilder();
    // // Delete node
    // mutationA.setTypeValue(2);
    // mutationA.setStartNode(nodeF.build());

    // Mutation.Builder mutationB = Mutation.newBuilder();
    // // Add node
    // mutationB.setTypeValue(0);
    // mutationB.setStartNode(nodeG.build());

    // Mutation.Builder mutationB1 = Mutation.newBuilder();
    // // Add edge from E to G
    // mutationB1.setTypeValue(1);
    // mutationB1.setStartNode(nodeE.build());
    // mutationB1.setEndNode(nodeG.build());

    // Mutation.Builder mutationC = Mutation.newBuilder();
    // // Add node
    // mutationC.setTypeValue(0);
    // mutationC.setStartNode(nodeH.build());

    // Mutation.Builder mutationC1 = Mutation.newBuilder();
    // mutationC1.setTypeValue(1);
    // mutationC1.setStartNode(nodeH.build());
    // mutationC1.setEndNode(nodeG.build());

    // Mutation.Builder mutationD = Mutation.newBuilder();
    // // Add edge
    // mutationD.setTypeValue(1);
    // mutationD.setStartNode(nodeD.build());
    // mutationD.setEndNode(nodeG.build());

    // MutationList.Builder mutationList = MutationList.newBuilder();
    // mutationList.addMutation(mutationA.build());
    // mutationList.addMutation(mutationB.build());
    // mutationList.addMutation(mutationB1.build());
    // mutationList.addMutation(mutationC.build());
    // mutationList.addMutation(mutationC1.build());
    // mutationList.addMutation(mutationD.build());

    // FileOutputStream output = new FileOutputStream("graph.txt");
    // graph.build().writeTo(output);
    // output.close();

    // FileOutputStream output1 = new FileOutputStream("mutations.txt");
    // mutationList.build().writeTo(output1);
    // output1.close();
  }

  /*
   * Called when a client submits a POST request to the /data URL Adds submitted
   * comment to internal record if the comment is non-empty.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }
}
