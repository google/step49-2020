// Copyright 2019 Google LLC
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
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.sps.data.Utility;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

import com.google.sps.data.DataGraph;
import com.google.sps.data.GraphNode;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public final class GraphGenerationTest {
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
    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());
    gNodeB = Utility.protoNodeToGraphNode(nodeB.build());
    gNodeC = Utility.protoNodeToGraphNode(nodeC.build());
    gNodeD = Utility.protoNodeToGraphNode(nodeD.build());
    gNodeE = Utility.protoNodeToGraphNode(nodeE.build());
    gNodeF = Utility.protoNodeToGraphNode(nodeF.build());
  }

  /*
   * Tests that a proto node with no tokens and metadata is correctly converted
   * into an empty graph node with the same name
   */
  @Test
  public void correctProtoToGraphNoData() {
    GraphNode graphNode = Utility.protoNodeToGraphNode(nodeA.build());
    Assert.assertEquals(graphNode.name(), "A");
    Assert.assertEquals(graphNode.tokenList().size(), 0);
    Assert.assertEquals(graphNode.metadata().getFieldsCount(), 0);
  }

  /*
   * Tests that a proto node with tokens and no metadata is correctly converted
   * into an graph node with the same name, same token list and empty metadata
   */
  @Test
  public void correctProtoToGraphWithData() {
    nodeA.addToken("1");
    nodeA.addToken("2");
    nodeA.addToken("3");

    GraphNode graphNode = Utility.protoNodeToGraphNode(nodeA.build());
    List<String> tokenList = graphNode.tokenList();

    Assert.assertEquals(graphNode.name(), "A");
    Assert.assertEquals(tokenList.size(), 3);
    Assert.assertEquals(tokenList.get(0), "1");
    Assert.assertEquals(tokenList.get(1), "2");
    Assert.assertEquals(tokenList.get(2), "3");
    Assert.assertEquals(graphNode.metadata().getFieldsCount(), 0);
  }

  /*
   * Tests that a proto node with tokens and metadata is correctly converted
   * into an graph node with the same name, same token list and same metadata
   */
  @Test
  public void correctProtoToGraphWithDataAndMetadata() {
    nodeA.addToken("1");
    nodeA.addToken("2");
    nodeA.addToken("3");

    Value rowValue = Value.newBuilder().setStringValue("10").build();
    Value colValue = Value.newBuilder().setStringValue("17").build();

    Struct metadata =
        Struct.newBuilder().putFields("row", rowValue).putFields("column", colValue).build();
    nodeA.setMetadata(metadata);

    GraphNode graphNode = Utility.protoNodeToGraphNode(nodeA.build());
    List<String> tokenList = graphNode.tokenList();
    Struct generatedMetadata = graphNode.metadata();

    Assert.assertEquals(graphNode.name(), "A");
    Assert.assertEquals(tokenList.size(), 3);
    Assert.assertEquals(tokenList.get(0), "1");
    Assert.assertEquals(tokenList.get(1), "2");
    Assert.assertEquals(tokenList.get(2), "3");
    Assert.assertEquals(generatedMetadata.getFieldsCount(), 2);
    Assert.assertEquals(generatedMetadata.getFieldsOrThrow("row").getStringValue(), "10");
    Assert.assertEquals(generatedMetadata.getFieldsOrThrow("column").getStringValue(), "17");
  }

  /*
   * Tests that a graph node is correctly deep-copied
   */
  @Test
  public void deepCopyGraphNode() {
    nodeA.addToken("1");
    nodeA.addToken("2");
    nodeA.addToken("3");

    Value rowValue = Value.newBuilder().setStringValue("10").build();
    Value colValue = Value.newBuilder().setStringValue("17").build();

    Struct metadata =
        Struct.newBuilder().putFields("row", rowValue).putFields("column", colValue).build();
    nodeA.setMetadata(metadata);

    GraphNode graphNode = Utility.protoNodeToGraphNode(nodeA.build());
    List<String> tokenList = graphNode.tokenList();
    metadata = graphNode.metadata();

    // Assert that a copy is a deep copy
    GraphNode nodeCopy = graphNode.getCopy();
    String copyName = nodeCopy.name();
    List<String> copyTokenList = nodeCopy.tokenList();
    Struct copyMetadata = nodeCopy.metadata();

    // Nodes should be equal content-wise but not the same pointers
    Assert.assertEquals(graphNode, nodeCopy);
    Assert.assertFalse(graphNode == nodeCopy);

    Assert.assertEquals(copyName, "A");
    Assert.assertEquals(tokenList, copyTokenList);
    Assert.assertFalse(tokenList == copyTokenList);
    Assert.assertEquals(metadata, copyMetadata);
    Assert.assertFalse(metadata == copyMetadata);
  }

  /*
   * Test that nodes and edges of the following graph are correctly added:
   *                              A
   *                            _/ \_
   *                            B -> C
   * Also test that nodes aren't added multiple times to the graph, for example
   * node C would be added first as a child of node A but shouldn't be added again
   * as a child of node B
   */
  @Test
  public void simpleGraph() {
    Map<String, Node> protoNodesMap = new HashMap<>();

    nodeA.addChildren("B");
    nodeA.addChildren("C");

    nodeB.addChildren("C");

    Node pNodeA = nodeA.build();
    Node pNodeB = nodeB.build();
    Node pNodeC = nodeC.build();

    protoNodesMap.put("A", pNodeA);
    protoNodesMap.put("B", pNodeB);
    protoNodesMap.put("C", pNodeC);

    gNodeA = Utility.protoNodeToGraphNode(pNodeA);
    gNodeB = Utility.protoNodeToGraphNode(pNodeB);
    gNodeC = Utility.protoNodeToGraphNode(pNodeC);

    DataGraph dataGraph = new DataGraph();
    boolean success = dataGraph.graphFromProtoNodes(protoNodesMap);
    Assert.assertTrue(success);

    MutableGraph<GraphNode> graph = dataGraph.getGraph();
    HashMap<String, GraphNode> graphNodesMap = dataGraph.getGraphNodesMap();

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));

    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeC));
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeB, gNodeC));
    Assert.assertFalse(graph.hasEdgeConnecting(gNodeC, gNodeB));
    Assert.assertFalse(graph.hasEdgeConnecting(gNodeC, gNodeA));
    Assert.assertFalse(graph.hasEdgeConnecting(gNodeB, gNodeA));

    Assert.assertEquals(graphNodesMap.size(), 3);
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);
  }

  /*
   * Ensure that getter fields for node map and roots of a data graph return deep copies
   * of the original fields
   */
  @Test
  public void ensureFieldsCopies() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    HashSet<String> roots = new HashSet<>();
    roots.add("A");
    roots.add("B");

    DataGraph dataGraph = new DataGraph(graph, graphNodesMap, roots, 0);

    // Make sure roots and nodes map returned are always copies of the original
    HashMap<String, GraphNode> graphNodesMapCopy = dataGraph.getGraphNodesMap();
    HashSet<String> rootsCopy = dataGraph.getRoots();
    Assert.assertEquals(graphNodesMap, graphNodesMapCopy);
    Assert.assertFalse(graphNodesMap == graphNodesMapCopy);
    Assert.assertEquals(graphNodesMap.get("A"), graphNodesMapCopy.get("A"));
    Assert.assertFalse(graphNodesMap.get("A") == graphNodesMapCopy.get("A"));
    Assert.assertEquals(roots, rootsCopy);
    Assert.assertFalse(roots == rootsCopy);
  }

  /*
   * Check that a cyclic graph is detected and an error is returned.
   */
  @Test
  public void notDAG() {
    Map<String, Node> protoNodesMap = new HashMap<>();

    nodeA.addChildren("B");

    nodeB.addChildren("A");

    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    DataGraph dataGraph = new DataGraph();
    boolean success = dataGraph.graphFromProtoNodes(protoNodesMap);
    Assert.assertFalse(success);
  }

  /*
   * Make sure a data graph's copy function returns a copy of the original data graph
   */
  @Test
  public void copyDataGraph() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    HashSet<String> roots = new HashSet<>();
    roots.add("A");
    roots.add("B");

    DataGraph dataGraph = new DataGraph(graph, graphNodesMap, roots, 0);
    DataGraph dataGraphCopy = dataGraph.getCopy();

    Assert.assertEquals(dataGraph, dataGraphCopy);
    Assert.assertFalse(dataGraph == dataGraphCopy);

    Assert.assertEquals(dataGraph.getGraph(), dataGraphCopy.getGraph());
    Assert.assertFalse(dataGraph.getGraph() == dataGraphCopy.getGraph());

    Assert.assertEquals(dataGraph.getRoots(), dataGraphCopy.getRoots());
    Assert.assertFalse(dataGraph.getRoots() == dataGraphCopy.getRoots());

    Assert.assertEquals(dataGraph.getGraphNodesMap(), dataGraphCopy.getGraphNodesMap());
    Assert.assertFalse(dataGraph.getGraphNodesMap() == dataGraphCopy.getGraphNodesMap());
  }
}
