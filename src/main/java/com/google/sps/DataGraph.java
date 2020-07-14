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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
        /* numMutations = */ 0);
  }

  /**
   * Create a new data graph with the given attributes
   *
   * @param graph the guava graph
   * @param graphNodesMap the map from node name to node
   * @param roots a set of roots (nodes with no in-edges) of the graph
   * @param numMutations the number of mutations applied to the initial graph to get this graph
   * @return the data graph with these attributes
   */
  static DataGraph create(
      MutableGraph<GraphNode> graph,
      HashMap<String, GraphNode> graphNodesMap,
      HashSet<String> roots,
      int numMutations) {
    return new AutoValue_DataGraph(graph, graphNodesMap, roots, numMutations);
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
   * @return the the number of mutations applied to the initial graph to get this graph
   */
  abstract int numMutations();

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
    HashSet<String> copyRoots = new HashSet<>();
    copyRoots.addAll(roots);
    return DataGraph.create(Graphs.copyOf(graph), copyMap, copyRoots, mutationNum);
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
   * @return true if the mutation was successfully applied, false otherwise
   */
  public boolean mutateGraph(Mutation mut) {
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

    Set<GraphNode> successors;

    switch (mut.getType()) {
      case ADD_NODE:
        // adding a duplicate node doesn't make any change
        if (!graphNodesMap.containsKey(startName)) {
          // New lone node is a root
          roots.add(startName);
          // Create a new node with the given name and add it to the graph and the map
          GraphNode newGraphNode =
              GraphNode.create(startName, new ArrayList<>(), Struct.newBuilder().build());
          graph.addNode(newGraphNode);
          graphNodesMap.put(startName, newGraphNode);
        }
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
        successors = graph.successors(startNode);
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
        GraphNode newNode = changeNodeToken(startNode, mut.getTokenChange());

        if (newNode == null) {
          return false;
        }

        graphNodesMap.put(startName, newNode);

        successors = graph.successors(startNode);
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
      default:
        // unrecognized mutation type
        return false;
    }
    return true;
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
    } else if (tokenMutType == TokenMutation.Type.DELETE_TOKEN) {
      tokenList.removeAll(tokenNames);
    } else {
      // unrecognized mutation
      return null;
    }
    GraphNode newNode = GraphNode.create(node.name(), tokenList, node.metadata());
    return newNode;
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
   * Returns a MutableGraph with nodes that are at most a certain radius from a given node If the
   * radius is less than 0 or the node specified isn't present, return an emptu graph Since maxDepth
   * was implemented as DFS, we use BFS for *diversity*
   *
   * @param name the name of the node to search for
   * @param radius the distance from the node to search for parents and children
   * @return a graph comprised of only nodes and edges within a certain distance from the specified
   *     node
   */
  public MutableGraph<GraphNode> getReachableNodes(String name, int radius) {
    if (radius < 0) {
      return GraphBuilder.directed().build(); // If max depth below 0, then return an emtpy graph
    }

    HashMap<String, GraphNode> graphNodesMap = this.graphNodesMap();

    if (!graphNodesMap.containsKey(name)) {
      return GraphBuilder.directed()
          .build(); // If the specified node is not found, return an empty graph
    }

    MutableGraph<GraphNode> graph = this.graph();
    GraphNode tgtNode = graphNodesMap.get(name);

    Map<GraphNode, Boolean> visited = new HashMap<>();

    HashSet<GraphNode> nextLayer;
    ArrayDeque<GraphNode> queue = new ArrayDeque<>();

    queue.add(tgtNode); // Adds the searched node to the queue

    for (int i = 0; i <= radius; i++) {
      // Break out early if queue is empty
      if (queue.size() == 0) {
        break;
      }
      nextLayer = new HashSet<>();
      while (!queue.isEmpty()) {
        GraphNode curr = queue.poll();

        if (!visited.containsKey(curr)) {
          visited.put(curr, true);

          // Adds the children
          for (GraphNode child : graph.successors(curr)) {
            if (!visited.containsKey(child)) {
              nextLayer.add(child);
            }
          }
          // Adds the parents
          for (GraphNode parent : graph.predecessors(curr)) {
            if (!visited.containsKey(parent)) {
              nextLayer.add(parent);
            }
          }
        }
      }
      queue.addAll(nextLayer);
    }
    MutableGraph<GraphNode> graphToReturn = Graphs.inducedSubgraph(graph, visited.keySet());

    return graphToReturn;
  }
}
