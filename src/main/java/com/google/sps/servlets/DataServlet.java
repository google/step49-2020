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
import java.util.ArrayDeque;
import java.util.ArrayList;
import com.google.protobuf.Struct;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

@WebServlet("/data")
public class DataServlet extends HttpServlet {
  // PROTO Data structure:
  // Parse the contents of graph.txt into a proto Graph object, and extract
  // information
  // from the proto object into a map. This is used to store the proto input and
  // isn't updated
  // with mutations.
  private Graph protoGraph = null;
  private Map<String, Node> protoNodesMap = null;

  // GRAPH Data structures:
  // Create an undirected graph data structure to store the information, and
  // map each node name in the graph to the GraphNode objects. This is the graph &
  // map
  // we update with mutations
  private MutableGraph<GraphNode> graph = null;
  private HashMap<String, GraphNode> graphNodesMap = null;

  private ImmutableGraph<GraphNode> graphOriginal = null; // never undergoes mutations

  // Data structure that stores the roots of the graph across mutations
  // Roots are nodes with no in-edges
  private HashSet<String> roots = null;

  private List<Mutation> mutList = null;

  private boolean mutationsApplied = false;

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
    if (protoGraph == null
        || protoNodesMap == null
        || graph == null
        || graphNodesMap == null
        || roots == null) {
      protoGraph = Graph.parseFrom(getServletContext().getResourceAsStream("/WEB-INF/graph.txt"));
      protoNodesMap = protoGraph.getNodesMapMap();

      graph = GraphBuilder.directed().build();
      graphNodesMap = new HashMap<>();

      roots = new HashSet<>();

      // Generate graph data structures from proto data structure
      success = graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);
      graphOriginal = ImmutableGraph.copyOf(graph); // Create a copy of graph as the original graph
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
    }

    // Only apply mutations once
    if (!mutationsApplied) {
      for (Mutation mut : mutList) {
        success = mutateGraph(mut, graph, graphNodesMap, roots);
        if (!success) {
          String error = "Failed to apply mutation " + mut.toString() + " to graph";
          response.setHeader("serverError", error);
          return;
        }
      }
      mutationsApplied = true;
    }

    MutableGraph<GraphNode> truncatedGraph =
        getGraphWithMaxDepth(graph, roots, graphNodesMap, depthNumber);
    String graphJson = graphToJson(truncatedGraph, roots);
    response.getWriter().println(graphJson);
  }

  /*
   * Takes in a map from node name to proto-parsed node object. Populates graph
   * with node and edge information and graphNodesMap with links from node names
   * to graph node objects.
   *
   * @param protNodesMap map from node name to proto Node object parsed from input
   *
   * @param graph Guava graph to fill with node and edge information
   *
   * @param graphNodesMap map object to fill with node-name -> graph node object
   * links
   *
   * @param roots the roots of the graph
   *
   * @return false if an error occurred, true otherwise
   */
  public boolean graphFromProtoNodes(
      Map<String, Node> protoNodesMap,
      MutableGraph<GraphNode> graph,
      Map<String, GraphNode> graphNodesMap,
      HashSet<String> roots) {

    for (String nodeName : protoNodesMap.keySet()) {
      Node thisNode = protoNodesMap.get(nodeName);

      // Convert thisNode into a graph node that may store additional information
      GraphNode graphNode = protoNodeToGraphNode(thisNode);

      // Update graph data structures to include the node as long as it doesn't
      // already exist
      if (!graphNodesMap.containsKey(nodeName)) {
        roots.add(nodeName);
        graph.addNode(graphNode);
        graphNodesMap.put(nodeName, graphNode);
      }

      // Add dependency edges to the graph
      for (String child : thisNode.getChildrenList()) {
        // This child can no longer be a root since it has an in-edge
        roots.remove(child);
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
   * element of the array contains a Json representation of the nodes of the graph
   * and the second a Json representation of the edges of the graph.
   *
   * @param graph the graph to convert into a JSON String
   */
  public String graphToJson(MutableGraph<GraphNode> graph, HashSet<String> roots) {
    Type typeOfNode = new TypeToken<Set<GraphNode>>() {}.getType();
    Type typeOfEdge = new TypeToken<Set<EndpointPair<GraphNode>>>() {}.getType();
    Type typeOfRoots = new TypeToken<Set<String>>() {}.getType();
    Gson gson = new Gson();
    String nodeJson = gson.toJson(graph.nodes(), typeOfNode);
    String edgeJson = gson.toJson(graph.edges(), typeOfEdge);
    String rootsJson = gson.toJson(roots, typeOfRoots);
    String bothJson =
        new JSONObject()
            .put("nodes", nodeJson)
            .put("edges", edgeJson)
            .put("roots", rootsJson)
            .toString();
    return bothJson;
  }

  /*
   * Converts a proto node object into a graph node object that does not store the
   * names of the child nodes but may store additional information.
   *
   * @param thisNode the input data Node object
   *
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
   *
   * @param mut the mutation to affect
   *
   * @param graph the Guava graph to mutate
   *
   * @param graphNodesMap a reference of existing nodes, also to be mutated
   *
   * @param roots the roots of the graph before the mutation
   *
   * @return true if the mutation was successful, false otherwise
   */
  public boolean mutateGraph(
      Mutation mut,
      MutableGraph<GraphNode> graph,
      Map<String, GraphNode> graphNodesMap,
      HashSet<String> roots) {
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
        // New lone node is a root
        roots.add(startName);
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
        // The target cannot be a root since it has an in-edge
        roots.remove(endName);
        graph.putEdge(startNode, endNode);
        break;
      case DELETE_NODE:
        if (startNode == null) { // Check node exists before removing
          return false;
        }
        // Check whether any successor will have no in-edges after this node is removed
        // If so, make them roots
        Set<GraphNode> successors = graph.successors(startNode);
        for (GraphNode succ : successors) {
          if (graph.inDegree(succ) == 1) {
            roots.add(succ.name());
          }
        }
        roots.remove(startName);
        graph.removeNode(startNode); // This will remove all edges associated with startNode
        graphNodesMap.remove(startName);
        break;
      case DELETE_EDGE:
        if (startNode == null || endNode == null) { // Check nodes exist before removing edge
          return false;
        }
        // If the target now has no in-edges, it becomes a root
        if (graph.inDegree(endNode) == 1) {
          roots.add(endName);
        }
        graph.removeEdge(startNode, endNode);
        break;
      case CHANGE_TOKEN:
        if (startNode == null) {
          return false;
        }
        return changeNodeToken(startNode, mut.getTokenChange());
      default:
        // unrecognized mutation type
        return false;
    }
    return true;
  }

  /*
   * Modify the list of tokens for graph node 'node' to accomodate the mutation
   * 'tokenMut'. This could involve adding or removing tokens from the list.
   *
   * @param node the node in the graph to change the tokens of
   *
   * @param tokenMut the kind of mutation to perform on node of the graph
   *
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

  /**
   * Given an input graph, roots (as strings), a node map (from string to Graph Nodes), and a
   * maximum depth, the function returns a graph with only nodes up to a max depth. Edges that don't
   * contain nodes both reachable up until the max depth are discarded.
   *
   * @param graphInput the input graph, as a Mutatable Graph
   * @param roots the name (string) of the roots
   * @param graphNodesMap a mapping of strings to GraphNodes
   * @param maxDepth the maximum depth of a node from a root
   * @return a graph with nodes only a certain distance from a root
   */
  public MutableGraph<GraphNode> getGraphWithMaxDepth(
      MutableGraph<GraphNode> graphInput,
      Set<String> roots,
      HashMap<String, GraphNode> graphNodesMap,
      int maxDepth) {

    MutableGraph<GraphNode> graphToReturn = GraphBuilder.directed().build();
    if (maxDepth < 0) {
      return graphToReturn; // If max depth below 0, then return an emtpy graph
    }
    ArrayDeque<GraphNode> queue = new ArrayDeque<GraphNode>();
    Map<GraphNode, Boolean> visited = new HashMap<>();

    for (String rootName : roots) {
      // Get the GraphNode object corresponding to the root name, add to the queue
      GraphNode rootNode = graphNodesMap.get(rootName);
      queue.add(rootNode);
    }

    HashSet<GraphNode> nextDepthNodes;

    // Process the graph layer by layer upto maxDepth
    for (int currDepth = 0; currDepth <= maxDepth; currDepth++) {
      nextDepthNodes = new HashSet<>();
      while (!queue.isEmpty()) {
        GraphNode curr = queue.poll();

        // Add to the graph to return
        if (!visited.containsKey(curr)) {
          graphToReturn.addNode(curr);
          visited.put(curr, true);

          // Add all the node's children to the next layer
          for (GraphNode gn : graphInput.successors(curr)) {
            if (!visited.containsKey(gn)) {
              nextDepthNodes.add(gn);
              // Add the corresponding edge if the next layer isn't too deep
            }
          }
        }
      }
      // make the next layer the new current layer
      queue.addAll(nextDepthNodes);
    }
    // Add the edges that we need, edges are only relevant if they contain nodes in
    // our graph
    for (EndpointPair<GraphNode> edge : graphInput.edges()) {
      if (graphToReturn.nodes().contains(edge.nodeU())
          && graphToReturn.nodes().contains(edge.nodeV())) {
        graphToReturn.putEdge(edge.nodeU(), edge.nodeV());
      }
    }
    return graphToReturn;
  }
}
