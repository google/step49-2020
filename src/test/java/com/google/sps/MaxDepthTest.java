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

import com.google.common.graph.*;
import com.google.sps.servlets.DataServlet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import com.google.sps.data.GraphNode;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MaxDepthTest {

  DataServlet servlet;

  // Proto nodes to construct graph with
  Builder nodeA = Node.newBuilder().setName("A");
  Builder nodeB = Node.newBuilder().setName("B");
  Builder nodeC = Node.newBuilder().setName("C");
  Builder nodeD = Node.newBuilder().setName("D");
  Builder nodeE = Node.newBuilder().setName("E");
  Builder nodeF = Node.newBuilder().setName("F");

  GraphNode gNodeA;
  GraphNode gNodeB;
  GraphNode gNodeC;
  GraphNode gNodeD;
  GraphNode gNodeE;
  GraphNode gNodeF;

  @Before
  public void setUp() {
    servlet = new DataServlet();
    gNodeA = servlet.protoNodeToGraphNode(nodeA.build());
    gNodeB = servlet.protoNodeToGraphNode(nodeB.build());
    gNodeC = servlet.protoNodeToGraphNode(nodeC.build());
    gNodeD = servlet.protoNodeToGraphNode(nodeC.build());
    gNodeE = servlet.protoNodeToGraphNode(nodeE.build());
    gNodeF = servlet.protoNodeToGraphNode(nodeF.build());
  }

  /** Max depth 0 should only return the roots */
  @Test
  public void maxDepthZero() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    MutableGraph<GraphNode> truncatedGraph =
        servlet.getGraphWithMaxDepth(graph, roots, graphNodesMap, 0);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();

    Assert.assertEquals(graphNodes.size(), 1);
    Assert.assertTrue(graphNodes.contains(gNodeA));
  }

  /** Invalid depth should not return anything */
  @Test
  public void invalidDepthEmptyGraph() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    MutableGraph<GraphNode> truncatedGraph =
        servlet.getGraphWithMaxDepth(graph, roots, graphNodesMap, -2);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();

    Assert.assertTrue(graphNodes.isEmpty());
  }

  /** Distance is equal to the max distance from root to a node, the entire graph is kept */
  @Test
  public void entireGraphIsKept() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    MutableGraph<GraphNode> truncatedGraph =
        servlet.getGraphWithMaxDepth(graph, roots, graphNodesMap, 1);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();

    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
  }

  /** Distance is greater than the max distance from root to a node, the entire graph is kept */
  @Test
  public void maxDepthIsGreaterThanMaxDistance() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    MutableGraph<GraphNode> truncatedGraph =
        servlet.getGraphWithMaxDepth(graph, roots, graphNodesMap, 2);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();

    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
  }

  /**
   * two ways to get to node E. One way is depth 3 and the other is depth 2. Node E should be in the
   * final graph
   */
  @Test
  public void testMultipleWaysToGetToNodeFindsShorter() {
    // A -> B -> D -> E
    // \> C ---------/> (C points to E)
    nodeA.addChildren("B");
    nodeA.addChildren("C");
    nodeB.addChildren("D");
    nodeD.addChildren("E");
    nodeC.addChildren("E");

    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.addNode(gNodeD);
    graph.addNode(gNodeE);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());
    protoNodesMap.put("E", nodeE.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    MutableGraph<GraphNode> truncatedGraph =
        servlet.getGraphWithMaxDepth(graph, roots, graphNodesMap, 2);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();

    Assert.assertEquals(graphNodes.size(), 5);
    Assert.assertTrue(graphNodes.contains(gNodeE));
  }

  /**
   * Multiple roots with max depth 0 should just return the roots
   */
  @Test
  public void multipleRootsZero() {
    nodeA.addChildren("B");
    nodeC.addChildren("D");

    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.addNode(gNodeD);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    MutableGraph<GraphNode> truncatedGraph =
        servlet.getGraphWithMaxDepth(graph, roots, graphNodesMap, 0);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();

    Assert.assertEquals(graphNodes.size(), 2);
  }

  /** More than one root node to calculate the depth from */
  @Test
  public void multipleRoots() {
    nodeA.addChildren("B");
    nodeB.addChildren("C");
    nodeC.addChildren("D");
    nodeE.addChildren("D");

    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.addNode(gNodeD);
    graph.addNode(gNodeE);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());
    protoNodesMap.put("E", nodeE.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    MutableGraph<GraphNode> truncatedGraph =
        servlet.getGraphWithMaxDepth(graph, roots, graphNodesMap, 1);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();

    Assert.assertEquals(graphNodes.size(), 4);
    Assert.assertFalse(graphNodes.contains(gNodeC));
  }
}
