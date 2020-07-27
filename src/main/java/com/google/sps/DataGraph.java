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

import com.google.auto.value.AutoValue;
import com.google.common.graph.*;
import com.google.protobuf.Struct;
import com.proto.GraphProtos.Node;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;

@AutoValue
abstract class DataGraph {

  /**
   * Create a new empty data graph
   *
   * @return the empty data graph with these attributes
   */
  public static DataGraph create() {
    return new AutoValue_DataGraph(
        /* graph = */ GraphBuilder.directed().build(),
        /* graphNodesMap = */ new HashMap<String, GraphNode>(),
        /* roots = */ new HashSet<String>(),
        /* numMutations = */ -1,
        /* tokenMap = */ new HashMap<String, Set<String>>());
  }

  /**
   * Create a new data graph with the given attributes
   *
   * @param graph the guava graph
   * @param graphNodesMap the map from node name to node
   * @param roots a set of roots (nodes with no in-edges) of the graph
   * @param numMutations the number of mutations applied to the initial graph to get this graph or
   *     -1 if no mutations have been applied
   * @return the data graph with these attributes
   */
  static DataGraph create(
      MutableGraph<GraphNode> graph,
      HashMap<String, GraphNode> graphNodesMap,
      HashSet<String> roots,
      int numMutations,
      HashMap<String, Set<String>> tokenMap) {
    return new AutoValue_DataGraph(graph, graphNodesMap, roots, numMutations, tokenMap);
  }

  /**
   * Getter for the graph
   *
   * @return the graph
   */
  abstract MutableGraph<GraphNode> graph();

  /**
   * Getter for the nodes map
   *
   * @return the map from node names -> nodes of the graph
   */
  abstract HashMap<String, GraphNode> graphNodesMap();

  /**
   * Getter for the roots
   *
   * @return the roots of the graph
   */
  abstract HashSet<String> roots();

  /**
   * Getter for the number of mutations
   *
   * @return the the number of mutations applied to the initial graph to get this graph or -1 if no
   *     mutations have been applied
   */
  abstract int numMutations();

  /**
   * Getter for the token map
   *
   * @return A map from token name to names of nodes with the token
   */
  abstract HashMap<String, Set<String>> tokenMap();

  /**
   * Return a shallow copy of the given data graph
   *
   * @return a shallow copy of the given data graph containing shallow copies of its attributes
   */
  public DataGraph getCopy() {
    MutableGraph<GraphNode> graph = this.graph();
    HashMap<String, GraphNode> graphNodesMap = this.graphNodesMap();
    HashSet<String> roots = this.roots();
    int mutationNum = this.numMutations();

    HashMap<String, GraphNode> copyMap = new HashMap<>();
    for (String key : graphNodesMap.keySet()) {
      copyMap.put(key, graphNodesMap.get(key));
    }

    HashMap<String, Set<String>> tokenCopyMap = new HashMap<>();
    for (String key : this.tokenMap().keySet()) {
      Set<String> nodesWithToken = new HashSet<>();
      nodesWithToken.addAll(this.tokenMap().get(key));
      tokenCopyMap.put(key, nodesWithToken);
    }
    HashSet<String> copyRoots = new HashSet<>();
    copyRoots.addAll(roots);
    return DataGraph.create(Graphs.copyOf(graph), copyMap, copyRoots, mutationNum, tokenCopyMap);
  }

  /**
   * Takes in a map from node name to proto-parsed node object. Populates data graph with
   * information from the parsed graph
   *
   * @param protoNodesMap map from node name to proto Node object parsed from input
   * @return false if an error occurred, true otherwise
   */
  boolean graphFromProtoNodes(Map<String, Node> protoNodesMap) {
    MutableGraph<GraphNode> graph = this.graph();
    HashMap<String, GraphNode> graphNodesMap = this.graphNodesMap();
    HashSet<String> roots = this.roots();

    for (String nodeName : protoNodesMap.keySet()) {
      Node thisNode = protoNodesMap.get(nodeName);
      // Convert thisNode into a graph node that may store additional information
      GraphNode graphNode = Utility.protoNodeToGraphNode(thisNode);

      // Update graph data structures to include the node as long as it doesn't
      // already exist
      if (!graphNodesMap.containsKey(nodeName)) {
        roots.add(nodeName);
        graph.addNode(graphNode);
        graphNodesMap.put(nodeName, graphNode);
      }

      // Store the token map
      for (String tokenName : thisNode.getTokenList()) {
        addToken(tokenName, nodeName);
      }

      // Add dependency edges to the graph
      for (String child : thisNode.getChildrenList()) {
        // This child can no longer be a root since it has an in-edge
        roots.remove(child);
        GraphNode childNode = Utility.protoNodeToGraphNode(protoNodesMap.get(child));
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

  /**
   * Applies a single mutation to the given data graph
   *
   * @param mut the mutation to apply to the graph
   * @return an empty string if there was no error, otherwise an error message
   */
  public String mutateGraph(Mutation mut) {
    MutableGraph<GraphNode> graph = this.graph();
    HashMap<String, GraphNode> graphNodesMap = this.graphNodesMap();
    HashSet<String> roots = this.roots();
    // Nodes affected by the mutation
    // second node only applicable for adding an edge and removing an edge
    String startName = mut.getStartNode();
    String endName = mut.getEndNode();

    // Getting the corresponding graph nodes from the graph map
    GraphNode startNode = graphNodesMap.get(startName);
    GraphNode endNode = graphNodesMap.get(endName);

    switch (mut.getType()) {
      case ADD_NODE:
        {
          // Check whether node to be added is a duplicate
          if (graphNodesMap.containsKey(startName)) {
            return "Add node: Adding a duplicate node " + startName + "\n";
          }
          // New lone node is a root
          roots.add(startName);
          // Create a new node with the given name and add it to the graph and the map
          GraphNode newGraphNode =
              GraphNode.create(startName, new ArrayList<>(), Struct.newBuilder().build());
          graph.addNode(newGraphNode);
          graphNodesMap.put(startName, newGraphNode);
          break;
        }
      case ADD_EDGE:
        {
          // Check nodes exist before adding an edge
          if (startNode == null) {
            return "Add edge: Start node " + startName + " doesn't exist\n";
          }
          if (endNode == null) {
            return "Add edge: End node " + endName + " doesn't exist\n";
          }

          // The target cannot be a root since it has an in-edge
          roots.remove(endName);
          graph.putEdge(startNode, endNode);
          break;
        }
      case DELETE_EDGE:
        {
          if (startNode == null) {
            return "Delete edge: Start node " + startName + " doesn't exist\n";
          }
          if (endNode == null) {
            return "Delete edge: End node " + endName + " doesn't exist\n";
          }

          graph.removeEdge(startNode, endNode);
          // If the target now has no in-edges, it becomes a root
          if (graph.inDegree(endNode) == 0) {
            roots.add(endName);
          }
          break;
        }
      case DELETE_NODE:
        {
          if (startNode == null) { // Check node exists before removing
            return "Delete node: Deleting a non-existent node " + startName + "\n";
          }
          for (String token : startNode.tokenList()) {
            removeToken(token, startName);
          }

          Set<GraphNode> successors = graph.successors(startNode);

          roots.remove(startName);
          graph.removeNode(startNode); // This will remove all edges associated with startNode
          graphNodesMap.remove(startName);

          // Check whether any successor will have no in-edges after this node is removed
          // If so, make them roots
          for (GraphNode succ : successors) {
            if (graph.inDegree(succ) == 0) {
              roots.add(succ.name());
            }
          }
          break;
        }
      case CHANGE_TOKEN:
        {
          if (startNode == null) {
            return "Change node: Changing a non-existent node " + startName + "\n";
          }
          GraphNode newNode = changeNodeToken(startNode, mut.getTokenChange());

          if (newNode == null) {
            return "Change node: Unrecognized token mutation "
                + mut.getTokenChange().getType()
                + "\n";
          }

          graphNodesMap.put(startName, newNode);

          Set<GraphNode> successors = graph.successors(startNode);
          Set<GraphNode> predecessors = graph.predecessors(startNode);
          graph.removeNode(startNode);

          graph.addNode(newNode);
          for (GraphNode succ : successors) {
            graph.putEdge(newNode, succ);
          }
          for (GraphNode pred : predecessors) {
            graph.putEdge(pred, newNode);
          }
          break;
        }
      default:
        // unrecognized mutation type
        return "Unrecognized mutation  " + mut.getType() + "\n";
    }
    return "";
  }

  /**
   * Modifies the list of tokens of this node to either add or remove tokens contained in tokenMut
   *
   * @param node the node whose token list should be modified
   * @param tokenMut the mutation that should be applied to the token list
   * @return the new GraphNode object, or null if it's an unrecognized mutation
   */
  private GraphNode changeNodeToken(GraphNode node, TokenMutation tokenMut) {

    // List of tokens to add/remove from the existing list
    List<String> tokenNames = tokenMut.getTokenNameList();
    // The existing list of tokens in the node
    List<String> tokenList = new ArrayList<>();
    tokenList.addAll(node.tokenList());

    TokenMutation.Type tokenMutType = tokenMut.getType();
    if (tokenMutType == TokenMutation.Type.ADD_TOKEN) {
      tokenList.addAll(tokenNames);

      // Update the map
      for (String tokenName : tokenNames) {
        addToken(tokenName, node.name());
      }
    } else if (tokenMutType == TokenMutation.Type.DELETE_TOKEN) {
      tokenList.removeAll(tokenNames);
      // Update the map
      for (String tokenName : tokenNames) {
        removeToken(tokenName, node.name());
      }
    } else {
      // unrecognized mutation
      return null;
    }
    return GraphNode.create(node.name(), tokenList, node.metadata());
  }

  /**
   * Adds a node to the tokenMap
   *
   * @param tokenName the token name (key in the map)
   * @param nodeName the node to add to the tokenName's set
   */
  private void addToken(String tokenName, String nodeName) {
    Set<String> nodesWithToken = this.tokenMap().getOrDefault(tokenName, new HashSet<>());
    nodesWithToken.add(nodeName);
    this.tokenMap().put(tokenName, nodesWithToken);
  }

  /**
   * Removes a node to the tokenMap
   *
   * @param tokenName the token name (key in the map)
   * @param nodeName the node to remove from the tokenName's set
   */
  private void removeToken(String tokenName, String nodeName) {
    if (this.tokenMap().containsKey(tokenName)) {
      Set<String> nodesWithToken = this.tokenMap().get(tokenName);
      nodesWithToken.remove(nodeName);
      if (nodesWithToken.size() == 0) { // No more nodes with token
        this.tokenMap().remove(tokenName);
      }
    } // Else no need to update
  }

  /**
   * Function for calculating nodes reachable from roots of this graph within at most maxDepth steps
   *
   * @param maxDepth the maximum depth of a node from a root
   * @return a graph with nodes only a certain distance from a root
   */
  public MutableGraph<GraphNode> getGraphWithMaxDepth(int maxDepth) {
    if (maxDepth < 0) {
      return GraphBuilder.directed().build(); // If max depth below 0, then return an emtpy graph
    }

    MutableGraph<GraphNode> graph = this.graph();
    HashMap<String, GraphNode> graphNodesMap = this.graphNodesMap();
    HashSet<String> roots = this.roots();
    Map<GraphNode, Boolean> visited = new HashMap<>();

    for (String rootName : roots) {
      GraphNode rootNode = graphNodesMap.get(rootName);
      dfsVisit(rootNode, visited, maxDepth);
    }
    MutableGraph<GraphNode> graphToReturn = Graphs.inducedSubgraph(graph, visited.keySet());

    return graphToReturn;
  }

  /**
   * Helper function for performing a depth-first traversal of the graph starting at node and adding
   * all those nodes to visited which are within depthRemaining steps from the node
   *
   * @param gn the GraphNode to start at
   * @param visited a map that records whether nodes have been visited
   * @param depthRemaining the number of layers left to explore, decreases by one with each
   *     recursive call on a child
   */
  private void dfsVisit(GraphNode gn, Map<GraphNode, Boolean> visited, int depthRemaining) {
    MutableGraph<GraphNode> graph = this.graph();
    if (depthRemaining >= 0) {
      visited.put(gn, true);
      for (GraphNode child : graph.successors(gn)) {
        if (!visited.containsKey(child)) {
          // Visit the child and indicate the increase in depth
          dfsVisit(child, visited, depthRemaining - 1);
        }
      }
    }
  }

  /**
   * Returns a MutableGraph from given nodes that are at most a certain radius from a given nodes.
   * If the radius is less than 0 or the nodes specified aren't present, return an empty graph.
   *
   * <p>Here, we only consider children of children and parents of parents. If a node doesn't exist
   * in the graph, we skip it. If all the nodes in the collection don't exist in the graph, we
   * return an empty graph.
   *
   * @param names the names of the nodes whose descendants within radius distance and all associated
   *     edges should be included in the graph
   * @param radius the distance from the node to search for parents and children
   * @return a graph comprised of only nodes and edges within a certain distance from the specified
   *     node. Empty if radius is less than 0 or if the node isn't found. Returns a graph with a
   *     depth of at most the radius when names is empty.
   */
  public MutableGraph<GraphNode> getReachableNodes(Collection<String> names, int radius) {
    if (radius < 0 || names == null) {
      return GraphBuilder.directed().build(); // If max depth below 0, then return an emtpy graph
    }

    HashMap<String, GraphNode> graphNodesMap = this.graphNodesMap();
    HashSet<GraphNode> nextLayerChildren = new HashSet<GraphNode>();
    HashSet<GraphNode> nextLayerParents = new HashSet<GraphNode>();

    if (names.size() == 0) {
      List<GraphNode> rootNodes =
          this.roots().stream()
              .map(rootName -> this.graphNodesMap().get(rootName))
              .collect(Collectors.toList());
      nextLayerChildren.addAll(rootNodes);
    }

    Set<GraphNode> nodesToAdd =
        names.stream()
            .filter(name -> graphNodesMap.containsKey(name))
            .map(name -> graphNodesMap.get(name))
            .collect(Collectors.toSet());
    nextLayerChildren.addAll(nodesToAdd);
    nextLayerParents.addAll(nodesToAdd);

    // None of the other nodes were found, so return empty
    if (nextLayerChildren.isEmpty()) {
      return GraphBuilder.directed().build();
    }

    MutableGraph<GraphNode> graph = this.graph();

    // HashSet should have expected O(1) lookup, changed from HashMap for space
    // Two different sets are needed because of cases where a node is both a parent
    // and a child
    HashSet<GraphNode> visitedChildren = new HashSet<>();
    HashSet<GraphNode> visitedParents = new HashSet<>();

    for (int i = 0; i <= radius; i++) {
      // Break out early if queue is empty
      if (nextLayerChildren.isEmpty() && nextLayerParents.isEmpty()) {
        break;
      }

      // Helper function used to avoid duplicate code
      nextLayerChildren = getNextLayer(nextLayerChildren, visitedChildren, true);
      nextLayerParents = getNextLayer(nextLayerParents, visitedParents, false);
    }

    HashSet<GraphNode> visited = new HashSet<>();
    visited.addAll(visitedChildren);
    visited.addAll(visitedParents);
    MutableGraph<GraphNode> graphToReturn = Graphs.inducedSubgraph(graph, visited);

    return graphToReturn;
  }

  /**
   * Helper function that gets the next layer of nodes based on what's visited, a queue, and whether
   * we're looking for children
   *
   * @param layer the layer of nodes to visit
   * @param visited A Hashset of visited nodes
   * @param isChild whether we're looking for children. True means we look for the children, and
   *     False means we look for parents.
   * @return the nodes relevant to the next layer
   */
  private HashSet<GraphNode> getNextLayer(
      HashSet<GraphNode> layer, HashSet<GraphNode> visited, boolean isChild) {
    HashSet<GraphNode> nextLayer = new HashSet<>();
    for (GraphNode curr : layer) {

      if (!visited.contains(curr)) {
        visited.add(curr);
        Set<GraphNode> adjacentNodes =
            isChild ? this.graph().successors(curr) : this.graph().predecessors(curr);
        for (GraphNode node : adjacentNodes) {
          if (!visited.contains(node)) {
            nextLayer.add(node);
          }
        }
      }
    }
    return nextLayer;
  }
}
