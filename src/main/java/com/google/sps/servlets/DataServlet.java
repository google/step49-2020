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

import java.util.List;
import java.util.Map;
import proto.GraphProtos.Graph;
import proto.GraphProtos.Node;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  /*
   * Called when a client submits a GET request to the /data URL
   * Displays all recorded user comments on page
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    /*
     * Code to read graph from test.txt file in same directory and print it
     * to the console
     */
    Graph graph = Graph.parseFrom(new FileInputStream("test.txt"));
    List<String> roots = graph.getRootNameList();
    System.out.println("Roots " + roots.toString());
    Map<String, Node> nodesMap = graph.getNodesMap();
    for(String nodeName : nodesMap.keySet()) {
      System.out.print("Node " + nodeName);
      Node thisNode = nodesMap.get(nodeName);
      System.out.println(" has children: " + thisNode.getChildrenList().toString());
    }
    /*
     * Code to build graph and write it to test.txt file in target directory
     */
    // Node.Builder nodeA = Node.newBuilder();
    // nodeA.setName("A");
    // nodeA.addChildren("B");

    // Node.Builder nodeB = Node.newBuilder();
    // nodeB.setName("B");
    // nodeB.addChildren("D");

    // Node.Builder nodeC = Node.newBuilder();
    // nodeB.setName("C");

    // Node.Builder nodeD = Node.newBuilder();
    // nodeB.setName("D");

    // Node.Builder nodeE = Node.newBuilder();
    // nodeE.setName("E");
    // nodeE.addChildren("F");

    // Node.Builder nodeF = Node.newBuilder();
    // nodeB.setName("F");

    // Graph.Builder graph = Graph.newBuilder();
    // graph.addRootName("A");
    // graph.addRootName("E");
    // graph.putNodesMap("A", nodeA.build());
    // graph.putNodesMap("B", nodeB.build());
    // graph.putNodesMap("C", nodeC.build());
    // graph.putNodesMap("D", nodeD.build());
    // graph.putNodesMap("E", nodeE.build());
    // graph.putNodesMap("F", nodeF.build());

    // FileOutputStream output = new FileOutputStream("test.txt");
    // graph.build().writeTo(output);
    // output.close();
  }


  /*
   * Called when a client submits a POST request to the /data URL
   * Adds submitted comment to internal record if the comment is
   * non-empty.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }
}
