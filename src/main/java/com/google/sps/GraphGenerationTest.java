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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
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

    DataGraph dataGraph = DataGraph.create();
    boolean success = dataGraph.graphFromProtoNodes(protoNodesMap);
    Assert.assertTrue(success);

    MutableGraph<GraphNode> graph = dataGraph.graph();
    HashMap<String, GraphNode> graphNodesMap = dataGraph.graphNodesMap();

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
   * Check that a cyclic graph is detected and an error is returned.
   */
  @Test
  public void notDAG() {
    Map<String, Node> protoNodesMap = new HashMap<>();

    nodeA.addChildren("B");

    nodeB.addChildren("A");

    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    DataGraph dataGraph = DataGraph.create();
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

    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);
    DataGraph dataGraphCopy = dataGraph.getCopy();

    Assert.assertEquals(dataGraph, dataGraphCopy);
    Assert.assertFalse(dataGraph == dataGraphCopy);

    Assert.assertEquals(dataGraph.graph(), dataGraphCopy.graph());
    Assert.assertFalse(dataGraph.graph() == dataGraphCopy.graph());

    Assert.assertEquals(dataGraph.roots(), dataGraphCopy.roots());
    Assert.assertFalse(dataGraph.roots() == dataGraphCopy.roots());

    Assert.assertEquals(dataGraph.graphNodesMap(), dataGraphCopy.graphNodesMap());
    Assert.assertFalse(dataGraph.graphNodesMap() == dataGraphCopy.graphNodesMap());
  }
}
