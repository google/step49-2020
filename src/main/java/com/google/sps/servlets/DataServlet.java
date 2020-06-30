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
    
    // PROTO Data structure: 
    // Parse the contents  of graph.txt into a proto Graph object, and extract information 
    // from the proto object into a map. This is used to store the proto input and isn't updated
    // with mutations.
    Graph protoGraph =
        Graph.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/graph.txt"));
    Map<String, Node> protoNodesMap = protoGraph.getNodesMapMap();
    
    // GRAPH Data structures:
    // Create an undirected graph data structure to store the information, and 
    // map each node name in the graph to the GraphNode objects. This is the graph & map
    // we update with mutations
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    // Create a graph (useable form) from proto data structures
    for (String nodeName : protoNodesMap.keySet()) {

      Node thisNode = protoNodesMap.get(nodeName);

      // Convert thisNode into a graph node that may store additional information
      GraphNode graphNode = protoNodeToGraphNode(thisNode);

      // Update graph data structures to include the node
      graph.addNode(graphNode);
      graphNodesMap.put(nodeName, graphNode);

      // Add dependency edges to the graph
      for (String child : thisNode.getChildrenList()) {
        graph.putEdge(graphNode, protoNodeToGraphNode(protoNodesMap.get(child)));
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

      // Getting the corresponding graph nodes from the graph map
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
          if (startNode != null && endNode != null) { // Check nodes exist before adding an edge
            graph.putEdge(startNode, endNode);
          }
          break;
        case DELETE_NODE:
          if (startNode != null) { // Check node exists before removing
            graph.removeNode(startNode); // This will remove all edges associated with startNode
            graphNodesMap.remove(startName);
          }
          break;
        case DELETE_EDGE:
          if (startNode != null && endNode != null) { // Check nodes exist before removing edge
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
   * @param thisNode the input data Node object
   * @return a useful node used to construct the Guava Graph
   */
  private GraphNode protoNodeToGraphNode(Node thisNode) {
    return GraphNode.create(thisNode.getName(), thisNode.getTokenList(), thisNode.getMetadata());
  }

  /*
   * Modify the list of tokens for graph node 'node' in 'graph' to accomodate
   * the mutation 'tokenMut'. This could involve adding or removing tokens
   * from the list.
   * @param graph the dependency graph the node belongs to
   * @param node the node in the graph to change the tokens of
   * @param tokenMut the kind of mutation to perform on node of the graph
   */
  private void changeNodeToken(
      MutableGraph<GraphNode> graph, GraphNode node, TokenMutation tokenMut) {
    // List of tokens to add/remove from the existing list
    List<String> tokenNames = tokenMut.getTokenNameList();
    // The existing list of tokens in the node
    List<String> tokenList = node.tokenList();
    int tokenMutType = tokenMut.getTypeValue();
    // 1 is enum value for adding, 2 is enum value for removing
    if (tokenMutType == 1) { 
      tokenList.addAll(tokenNames);
    } else if (tokenMutType == 2) {
      tokenList.removeAll(tokenNames);
    }
  }
}
