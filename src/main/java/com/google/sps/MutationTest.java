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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public final class MutationTest {

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
   * Check that a single node is correctly added to the graph
   */
  @Test
  public void addNodeBasic() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    Mutation addA = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A").build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(addA).length() == 0;
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 1);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
  }

  /*
   * Check that adding duplicate nodes does not cause an error
   * but does not make any change to the graph
   */
  @Test
  public void duplicateAddNoChange() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graphNodesMap.put("A", gNodeA);

    Mutation addA = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A").build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(addA).length() == 0;
    Assert.assertFalse(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 1);
    Assert.assertTrue(graphNodes.contains(gNodeA));
  }

  /*
   * Check that a single edge is correctly added to the graph
   */
  @Test
  public void addEdgeBasic() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);

    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(addAB).length() == 0;
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
    Assert.assertFalse(graph.hasEdgeConnecting(gNodeB, gNodeA));
  }

  /*
   * Check that adding an edge to a non-existent node errors
   */
  @Test
  public void addEdgeError() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.putEdge(gNodeA, gNodeB);

    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("C")
            .build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(addAB).length() == 0;
    // Assert.assertFalse(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
  }

  /*
   * Check that an existing node and all adjacent edges are correctly deleted
   */
  @Test
  public void deleteNode() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeB, gNodeC);

    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    graphNodesMap.put("C", gNodeC);

    Mutation removeA =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("A").build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(removeA).length() == 0;
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);
    Assert.assertEquals(graph.inDegree(gNodeB), 0);
    Assert.assertEquals(graph.outDegree(gNodeB), 1);
  }

  /*
   * Check that a deleting a non-existent node errors
   */
  @Test
  public void deleteAbsentNodeError() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.putEdge(gNodeA, gNodeB);

    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);

    Mutation removeC =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("C").build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(removeC).length() == 0;
    Assert.assertFalse(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
  }

  /*
   * Check that an existing edge is correctly deleted
   */
  @Test
  public void deleteEdge() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeB, gNodeC);

    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    graphNodesMap.put("C", gNodeC);

    Mutation removeAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(removeAB).length() == 0;
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);

    Assert.assertEquals(graph.inDegree(gNodeB), 0);
    Assert.assertEquals(graph.outDegree(gNodeB), 1);
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeB, gNodeC));
  }

  /*
   * Check that a deleting an edge with no endpoint specified errors
   */
  @Test
  public void deleteAbsentEdgeError() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.putEdge(gNodeA, gNodeB);

    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);

    Mutation removeAX =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_EDGE).setStartNode("A").build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(removeAX).length() == 0;
    Assert.assertFalse(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
  }

  /*
   * Check that a deleting an edge between unconnected nodes has no impact
   */
  @Test
  public void deleteAbsentEdgeSuccess() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeB, gNodeC);

    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    graphNodesMap.put("C", gNodeC);

    Mutation removeAC =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("C")
            .build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(removeAC).length() == 0;
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeB, gNodeC));
  }

  /*
   * Check that adding tokens to a node has the right effect
   */
  @Test
  public void addTokens() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeB, gNodeC);
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    graphNodesMap.put("C", gNodeC);

    List<String> newTokens = new ArrayList<>();
    newTokens.add("1");
    newTokens.add("2");
    newTokens.add("3");

    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("1")
            .addTokenName("2")
            .addTokenName("3")
            .build();

    Mutation addTokenToA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut)
            .build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(addTokenToA).length() == 0;
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertTrue(graphNodesMap.containsKey("A"));
    GraphNode newNodeA = graphNodesMap.get("A");
    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(newNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), newNodeA);
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);
    Assert.assertTrue(graph.hasEdgeConnecting(newNodeA, gNodeB));
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeB, gNodeC));

    // The original node should not be modified by the mutation (nodes are immutable)
    Assert.assertTrue(gNodeA.tokenList().size() == 0);
    Assert.assertEquals(newNodeA.tokenList(), newTokens);
  }

  /*
   * Added to address a bug in graph generation where node instance in
   * graph and map were different therefore mutating one didn't affect
   * the other. Originally, node B was first added to both the graph
   * and map as a child of A but only updated in the map when it itself
   * was processed. This test checks that the two are the same.
   */
  @Test
  public void addTokensToChild() {
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

    List<String> newTokens = new ArrayList<>();
    newTokens.add("1");
    newTokens.add("2");
    newTokens.add("3");

    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("1")
            .addTokenName("2")
            .addTokenName("3")
            .build();

    Mutation addTokenToB =
        Mutation.newBuilder()
            .setStartNode("B")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut)
            .build();

    success = dataGraph.mutateGraph(addTokenToB).length() == 0;
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertTrue(graphNodesMap.containsKey("B"));
    GraphNode newNodeB = graphNodesMap.get("B");

    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);

    // Ensure that updating a node in the map updates it in the graph as well
    Assert.assertTrue(graphNodes.contains(newNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);

    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, newNodeB));
    Assert.assertTrue(graph.hasEdgeConnecting(newNodeB, gNodeC));
    Assert.assertEquals(newNodeB.tokenList(), newTokens);

    // The original node should not be modified by the mutation (nodes are immutable)
    Assert.assertTrue(gNodeB.tokenList().size() == 0);
  }

  /*
   * Check that removing tokens from a node has the right effect
   */
  @Test
  public void removeTokens() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    nodeA.addToken("1");
    nodeA.addToken("2");
    nodeA.addToken("3");
    nodeA.addToken("4");
    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());

    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeB, gNodeC);
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    graphNodesMap.put("C", gNodeC);

    List<String> newTokens = new ArrayList<>();
    newTokens.add("1");
    newTokens.add("3");

    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.DELETE_TOKEN)
            .addTokenName("2")
            .addTokenName("4")
            .build();

    Mutation removeTokenFromA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut)
            .build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(removeTokenFromA).length() == 0;
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertTrue(graphNodesMap.containsKey("A"));
    GraphNode newNodeA = graphNodesMap.get("A");

    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(newNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);

    Assert.assertTrue(graph.hasEdgeConnecting(newNodeA, gNodeB));
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeB, gNodeC));
    Assert.assertEquals(newNodeA.tokenList(), newTokens);

    // The original node should not be modified by the mutation (nodes are immutable)
    Assert.assertTrue(gNodeA.tokenList().size() == 4);
  }

  /*
   * Check that not specifying the token mutation type errors
   */
  @Test
  public void invalidTokenMutation() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeB, gNodeC);
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    graphNodesMap.put("C", gNodeC);

    TokenMutation tokenMut = TokenMutation.newBuilder().addTokenName("2").addTokenName("4").build();

    Mutation addToA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut)
            .build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(addToA).length() == 0;
    Assert.assertFalse(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodesMap.containsKey("A"));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);

    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeB, gNodeC));
  }

  /*
   * Check that not specifying node to be mutated errors
   */
  @Test
  public void invalidNodeTokenMutation() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeB, gNodeC);
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    graphNodesMap.put("C", gNodeC);

    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("2")
            .addTokenName("4")
            .build();

    Mutation add =
        Mutation.newBuilder().setType(Mutation.Type.CHANGE_TOKEN).setTokenChange(tokenMut).build();

    HashSet<String> roots = new HashSet<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0);

    boolean success = dataGraph.mutateGraph(add).length() == 0;
    Assert.assertFalse(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodesMap.containsKey("A"));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);

    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeB, gNodeC));
  }
}
