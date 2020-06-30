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
import com.google.sps.data.GraphNode;
import com.proto.GraphProtos.Graph;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.MutationList;
import com.proto.MutationProtos.TokenMutation;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  /*
   * Called when a client submits a GET request to the /data URL
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Parse the contents  of graph.txt into a proto Graph object
    Graph inputGraph =
        Graph.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/graph.txt"));
    // Create a graph data structure to store the information
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();

    // Extract the graph information from the proto object
    Map<String, Node> nodesMap = inputGraph.getNodesMapMap();

    // Create an object that maps each node name in the current graph to the node object
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    for (String nodeName : nodesMap.keySet()) {

      Node thisNode = nodesMap.get(nodeName);

      // Convert thisNode into a graph node that may store additional information
      GraphNode graphNode = protoNodeToGraphNode(thisNode);

      // Add node to graph DS
      graph.addNode(graphNode);

      // Add link between node name and node in the map
      graphNodesMap.put(nodeName, graphNode);

      // Add dependency edges to the graph
      for (String child : thisNode.getChildrenList()) {
        graph.putEdge(graphNode, protoNodeToGraphNode(nodesMap.get(child)));
      }
    }

    // Parse the contents of mutation.txt into a list of mutations
    List<Mutation> mutList =
        MutationList.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/mutations.txt"))
            .getMutationList();

    for (Mutation mut : mutList) {
      // Name of the first node
      String startName = mut.getStartNode();
      // Name of the second node (only applicable for adding an edge and removing an edge)
      String endName = mut.getEndNode();

      // Getting the corresponding graph nodes from the map
      GraphNode startNode = graphNodesMap.get(startName);
      GraphNode endNode = graphNodesMap.get(endName);

      switch (mut.getType()) {
        case ADD_NODE:
          // Create a new node with the given name and add it to the graph and the map
          Node newNode = Node.newBuilder().setName(startName).build();
          GraphNode newGraphNode = protoNodeToGraphNode(newNode);
          graph.addNode(newGraphNode);
          graphNodesMap.put(startName, newGraphNode);
          break;
        case ADD_EDGE:
          // If the specified nodes exist in the graph, add an edge between them
          if (startNode != null && endNode != null) {
            graph.putEdge(startNode, endNode);
          }
          break;
        case DELETE_NODE:
          // If the specified node exists in the graph, remove it from the graph and the map
          if (startNode != null) {
            graph.removeNode(startNode);
            graphNodesMap.remove(startName);
          }
          break;
        case DELETE_EDGE:
          // If the specified nodes exist in the graph, remove the edge between them
          if (startNode != null && endNode != null) {
            graph.removeEdge(startNode, endNode);
          }
          break;
        case CHANGE_TOKEN:
          changeNodeToken(graph, startNode, mut.getTokenChange());
          break;
        default:
          break;
      }
    }
  }

  /*
   * Converts a proto node object into a graph node object that does not store the names of
   * the child nodes but may store additional information.
   */
  private GraphNode protoNodeToGraphNode(Node thisNode) {
    return GraphNode.create(thisNode.getName(), thisNode.getTokenList(), thisNode.getMetadata());
  }

  /*
   * Modify the list of tokens for graph node 'node' in 'graph' to accomodate
   * the mutation 'tokenMut'. This could involve adding or removing tokens
   * from the list.
   */
  private void changeNodeToken(
      MutableGraph<GraphNode> graph, GraphNode node, TokenMutation tokenMut) {
    List<String> tokenNames = tokenMut.getTokenNameList();
    List<String> tokenList = node.tokenList();
    int tokenMutType = tokenMut.getTypeValue();
    if (tokenMutType == 1) {
      // Add tokens
      tokenList.addAll(tokenNames);
    } else if (tokenMutType == 2) {
      // Remove tokens
      tokenList.removeAll(tokenNames);
    }
  }
}
