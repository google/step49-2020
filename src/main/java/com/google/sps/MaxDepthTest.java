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

import java.util.HashMap;
import java.util.Set;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableGraph;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.sps.Utility.protoNodeToGraphNode;

@RunWith(JUnit4.class)
public class MaxDepthTest {

  // Proto nodes to construct graph with
  Builder nodeA = Node.newBuilder().setName("A");
  Builder nodeB = Node.newBuilder().setName("B");
  Builder nodeC = Node.newBuilder().setName("C");
  Builder nodeD = Node.newBuilder().setName("D");
  Builder nodeE = Node.newBuilder().setName("E");
  Builder nodeF = Node.newBuilder().setName("F");
  Builder nodeG = Node.newBuilder().setName("G");
  Builder nodeH = Node.newBuilder().setName("H");

  GraphNode gNodeA;
  GraphNode gNodeB;
  GraphNode gNodeC;
  GraphNode gNodeD;
  GraphNode gNodeE;
  GraphNode gNodeF;
  GraphNode gNodeG;
  GraphNode gNodeH;

  @Before
  public void setUp() {
    gNodeA = protoNodeToGraphNode(nodeA.build());
    gNodeB = protoNodeToGraphNode(nodeB.build());
    gNodeC = protoNodeToGraphNode(nodeC.build());
    gNodeD = protoNodeToGraphNode(nodeD.build());
    gNodeE = protoNodeToGraphNode(nodeE.build());
    gNodeF = protoNodeToGraphNode(nodeF.build());
    gNodeG = protoNodeToGraphNode(nodeG.build());
    gNodeH = protoNodeToGraphNode(nodeH.build());
  }

  /** Max depth 0 should only return the roots */
  @Test
  public void maxDepthZero() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getGraphWithMaxDepth(0);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(graphNodes.size(), 1);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertFalse(graphNodes.contains(gNodeB));
    Assert.assertFalse(graphNodes.contains(gNodeC));

    Assert.assertEquals(graphEdges.size(), 0);
  }

  /** Invalid depth should not return anything */
  @Test
  public void invalidDepthEmptyGraph() {

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getGraphWithMaxDepth(-2);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertTrue(graphNodes.isEmpty());
    Assert.assertTrue(graphEdges.isEmpty());
  }

  /** Distance is equal to the max distance from root to a node, the entire graph is kept */
  @Test
  public void entireGraphIsKept() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getGraphWithMaxDepth(1);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));

    Assert.assertEquals(graphEdges.size(), 2);
  }

  /** Distance is greater than the max distance from root to a node, the entire graph is kept */
  @Test
  public void maxDepthIsGreaterThanMaxDistance() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getGraphWithMaxDepth(2);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();

    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));

    Assert.assertEquals(graphEdges.size(), 2);
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

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());
    protoNodesMap.put("E", nodeE.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getGraphWithMaxDepth(2);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(graphNodes.size(), 5);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertTrue(graphNodes.contains(gNodeD));
    Assert.assertTrue(graphNodes.contains(gNodeE));

    Assert.assertEquals(graphEdges.size(), 5);
  }

  /** Multiple roots with max depth 0 should just return the roots */
  @Test
  public void multipleRootsZero() {
    nodeA.addChildren("B");
    nodeC.addChildren("D");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getGraphWithMaxDepth(0);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeC));

    Assert.assertEquals(graphEdges.size(), 0);
  }

  /** More than one root node to calculate the depth from, algorithm will find shortest path */
  @Test
  public void multipleRoots() {
    nodeA.addChildren("B");
    nodeB.addChildren("C");
    nodeC.addChildren("D");
    nodeE.addChildren("D");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());
    protoNodesMap.put("E", nodeE.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getGraphWithMaxDepth(1);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(graphNodes.size(), 4);
    Assert.assertFalse(graphNodes.contains(gNodeC));
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeD));
    Assert.assertTrue(graphNodes.contains(gNodeE));

    Assert.assertEquals(graphEdges.size(), 2);
  }

  /** This test mirrors the example graph we have in graph.txt after the mutations specified. */
  @Test
  public void complexGraph() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");
    nodeB.addChildren("D");
    nodeD.addChildren("G");
    nodeE.addChildren("G");
    nodeH.addChildren("G");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());
    protoNodesMap.put("E", nodeE.build());
    protoNodesMap.put("G", nodeG.build());
    protoNodesMap.put("H", nodeH.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    MutableGraph<GraphNode> graph = dataGraph.graph();

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getGraphWithMaxDepth(1);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(graphNodes.size(), 6);
    Assert.assertFalse(graphNodes.contains(gNodeD));
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertTrue(graphNodes.contains(gNodeE));
    Assert.assertTrue(graphNodes.contains(gNodeG));
    Assert.assertTrue(graphNodes.contains(gNodeH));

    Assert.assertEquals(graphEdges.size(), 4);

    // Test encapsulation, original graph isn't modified
    Assert.assertEquals(graph.nodes().size(), 7);
    Assert.assertEquals(graph.edges().size(), 6);
  }
}
