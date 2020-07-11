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
import com.proto.GraphProtos.Node;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

@AutoValue
abstract class DataGraph {

  /**
   * Create a new empty data graph
   * 
   * @return the empty data graph with these attributes
   */
  public static DataGraph create() {
    return new AutoValue_DataGraph(GraphBuilder.directed().build(), new HashMap<String, GraphNode>(), new HashSet<String>());
  }

  /**
   * Create a new data graph with the given attributes
   * 
   * @param graph the guava graph
   * @param graphNodesMap the map from node name to node
   * @param roots a set of roots (nodes with no in-edges) of the graph
   * 
   * @return the data graph with these attributes
   */
   static DataGraph create(MutableGraph<GraphNode> graph, HashMap<String, GraphNode> graphNodesMap, HashSet<String> roots) {
    return new AutoValue_DataGraph(graph, graphNodesMap, roots);
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
   * Takes in a map from node name to proto-parsed node object. Populates data graph 
   * with information from the parsed graph
   *
   * @param protoNodesMap map from node name to proto Node object parsed from input
   * 
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
}
