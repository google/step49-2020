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
import com.google.gson.Gson;
import com.google.sps.data.GraphNode;
import com.proto.GraphProtos.Graph;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.MutationList;
import com.proto.MutationProtos.TokenMutation;
import com.google.common.graph.EndpointPair;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.google.protobuf.Struct;
import java.util.Map;
import java.util.Set;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
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
    response.setContentType("application/json");

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

    // Generate graph data structures from proto data structure
    boolean success = graphFromProtoNodes(protoNodesMap, graph, graphNodesMap);

    if (!success) {
      String error = "Failed to parse input graph into Guava graph - not a DAG!";
      response.setHeader("serverError", error);
      return;
    }

    // Parse the contents of mutation.txt into a list of mutations
    List<Mutation> mutList =
        MutationList.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/mutations.txt"))
            .getMutationList();

    for (Mutation mut : mutList) {
      success = mutateGraph(mut, graph, graphNodesMap);
      if (!success) {
        String error = "Failed to apply mutation " + mut.toString() + " to graph";
        response.setHeader("serverError", error);
        return;
      }
    }
    String graphJson = graphToJson(graph);
    response.getWriter().println(graphJson);
  }

  /*
   * Takes in a map from node name to proto-parsed node object. Populates graph with node and edge
   * information and graphNodesMap with links from node names to graph node objects.
   * @param protNodesMap map from node name to proto Node object parsed from input
   * @param graph Guava graph to fill with node and edge information
   * @param graphNodesMap map object to fill with node-name -> graph node object links
   * @return false if an error occurred, true otherwise
   */
  public boolean graphFromProtoNodes(
      Map<String, Node> protoNodesMap,
      MutableGraph<GraphNode> graph,
      Map<String, GraphNode> graphNodesMap) {

    for (String nodeName : protoNodesMap.keySet()) {

      Node thisNode = protoNodesMap.get(nodeName);

      // Convert thisNode into a graph node that may store additional information
      GraphNode graphNode = protoNodeToGraphNode(thisNode);

      // Update graph data structures to include the node
      if (!graphNodesMap.containsKey(nodeName)) {
        graph.addNode(graphNode);
        graphNodesMap.put(nodeName, graphNode);
      }

      // Add dependency edges to the graph
      for (String child : thisNode.getChildrenList()) {
        GraphNode childNode = protoNodeToGraphNode(protoNodesMap.get(child));
        if (!graphNodesMap.containsKey(child)) {
          // If child node is not already in the graph, add it
          graph.addNode(childNode);
          graphNodesMap.put(child, childNode);
        } else if (graph.hasEdgeConnecting(childNode, graphNode)) {
          // the graph is not a DAG, so we error out
          return false;
        }
        graph.putEdge(graphNode, childNode);
      }
    }
    return true;
  }

  /*
   * Converts a Guava graph into a String encoding of a Json Array. The first
   * element of the array contains a Json representation of the nodes of the
   * graph and the second a Json representation of the edges of the graph.
   * @param graph the graph to convert into a JSON String
   */
  private String graphToJson(MutableGraph<GraphNode> graph) {
    Type typeOfNode = new TypeToken<Set<GraphNode>>() {}.getType();
    Type typeOfEdge = new TypeToken<Set<EndpointPair<GraphNode>>>() {}.getType();
    Gson gson = new Gson();
    String nodeJson = gson.toJson(graph.nodes(), typeOfNode);
    String edgeJson = gson.toJson(graph.edges(), typeOfEdge);
    String bothJson = "[" + nodeJson + "," + edgeJson + "]";
    return bothJson;
  }

  /*
   * Converts a proto node object into a graph node object that does not store the names of
   * the child nodes but may store additional information.
   * @param thisNode the input data Node object
   * @return a useful node used to construct the Guava Graph
   */
  public GraphNode protoNodeToGraphNode(Node thisNode) {
    List<String> newTokenList = new ArrayList<>();
    newTokenList.addAll(thisNode.getTokenList());
    Struct newMetadata = Struct.newBuilder().mergeFrom(thisNode.getMetadata()).build();
    return GraphNode.create(thisNode.getName(), newTokenList, newMetadata);
  }

  /*
   * Changes the graph according to the given mutation object
   * @param mut the mutation to affect
   * @param graph the Guava graph to mutate
   * @param graphNodesMap a reference of existing nodes, also to be mutated
   * @return true if the mutation was successful, false otherwise
   */
  public boolean mutateGraph(
      Mutation mut, MutableGraph<GraphNode> graph, Map<String, GraphNode> graphNodesMap) {
    // Nodes affected by the mutation
    // second node only applicable for adding an edge and removing an edge
    String startName = mut.getStartNode();
    String endName = mut.getEndNode();

    // Getting the corresponding graph nodes from the graph map
    GraphNode startNode = graphNodesMap.get(startName);
    GraphNode endNode = graphNodesMap.get(endName);

    switch (mut.getType()) {
      case ADD_NODE:
        if (graphNodesMap.containsKey(startName)) {
          // adding a duplicate node
          return false;
        }
        // Create a new node with the given name and add it to the graph and the map
        GraphNode newGraphNode =
            GraphNode.create(startName, new ArrayList<>(), Struct.newBuilder().build());
        graph.addNode(newGraphNode);
        graphNodesMap.put(startName, newGraphNode);
        break;
      case ADD_EDGE:
        if (startNode == null || endNode == null) { // Check nodes exist before adding an edge
          return false;
        }
        graph.putEdge(startNode, endNode);
        break;
      case DELETE_NODE:
        if (startNode == null) { // Check node exists before removing
          return false;
        }
        graph.removeNode(startNode); // This will remove all edges associated with startNode
        graphNodesMap.remove(startName);
        break;
      case DELETE_EDGE:
        if (startNode == null || endNode == null) { // Check nodes exist before removing edge
          return false;
        }
        graph.removeEdge(startNode, endNode);
        break;
      case CHANGE_TOKEN:
        if (startNode == null) {
          return false;
        }
        boolean succ = changeNodeToken(startNode, mut.getTokenChange());
        return succ;
      default:
        // unrecognized mutation type
        return false;
    }
    return true;
  }

  /*
   * Modify the list of tokens for graph node 'node' to accomodate
   * the mutation 'tokenMut'. This could involve adding or removing tokens
   * from the list.
   * @param node the node in the graph to change the tokens of
   * @param tokenMut the kind of mutation to perform on node of the graph
   * @return true if the change is successful, false otherwise
   */
  private boolean changeNodeToken(GraphNode node, TokenMutation tokenMut) {
    // List of tokens to add/remove from the existing list
    List<String> tokenNames = tokenMut.getTokenNameList();
    // The existing list of tokens in the node
    List<String> tokenList = node.tokenList();
    TokenMutation.Type tokenMutType = tokenMut.getType();
    if (tokenMutType == TokenMutation.Type.ADD_TOKEN) {
      tokenList.addAll(tokenNames);
    } else if (tokenMutType == TokenMutation.Type.DELETE_TOKEN) {
      tokenList.removeAll(tokenNames);
    } else {
      // unrecognized mutation
      return false;
    }
    return true;
  }
}
