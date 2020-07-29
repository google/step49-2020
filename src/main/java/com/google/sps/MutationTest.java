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

    Mutation.Builder addA = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(addA);
    Assert.assertEquals(error.length(), 0);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 1);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
  }

  /*
   * Check that adding duplicate nodes gives an error message but does not make
   * any change to the graph
   */
  @Test
  public void duplicateAddNoChange() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graphNodesMap.put("A", gNodeA);

    Mutation.Builder addA = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(addA);
    Assert.assertEquals(error, "Add node: Adding a duplicate node A\n");

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

    Mutation.Builder addAB =
        Mutation.newBuilder().setType(Mutation.Type.ADD_EDGE).setStartNode("A").setEndNode("B");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(addAB);
    Assert.assertEquals(error.length(), 0);

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

    Mutation.Builder addAB =
        Mutation.newBuilder().setType(Mutation.Type.ADD_EDGE).setStartNode("A").setEndNode("C");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(addAB);
    Assert.assertEquals(error, "Add edge: End node C doesn't exist\n");

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

    Mutation.Builder removeA =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("A");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(removeA);
    Assert.assertEquals(error.length(), 0);

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

    Mutation.Builder removeC =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("C");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(removeC);
    Assert.assertEquals(error, "Delete node: Deleting a non-existent node C\n");

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

    Mutation.Builder removeAB =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_EDGE).setStartNode("A").setEndNode("B");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(removeAB);
    Assert.assertEquals(error.length(), 0);

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

    Mutation.Builder removeAX =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_EDGE).setStartNode("A");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(removeAX);
    Assert.assertEquals(error, "Delete edge: End node  doesn't exist\n");

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

    Mutation.Builder removeAC =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_EDGE).setStartNode("A").setEndNode("C");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(removeAC);
    Assert.assertEquals(error.length(), 0);

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

    Mutation.Builder addTokenToA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut);

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();

    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);
    HashMap<String, Set<String>> tokenMapNew = dataGraph.tokenMap();

    String error = dataGraph.mutateGraph(addTokenToA);
    Assert.assertEquals(error.length(), 0);

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

    // Token map
    Assert.assertTrue(tokenMapNew.containsKey("1"));
    Assert.assertTrue(tokenMapNew.containsKey("2"));
    Assert.assertTrue(tokenMapNew.containsKey("3"));
    Assert.assertTrue(tokenMapNew.get("1").contains("A"));
    Assert.assertEquals(1, tokenMapNew.get("1").size());
    Assert.assertTrue(tokenMapNew.get("2").contains("A"));
    Assert.assertEquals(1, tokenMapNew.get("2").size());
    Assert.assertTrue(tokenMapNew.get("3").contains("A"));
    Assert.assertEquals(1, tokenMapNew.get("3").size());

    // The original node should not be modified by the mutation (nodes are
    // immutable)
    Assert.assertTrue(gNodeA.tokenList().size() == 0);
    Assert.assertEquals(newNodeA.tokenList(), newTokens);
  }

  /*
   * Added to address a bug in graph generation where node instance in graph and
   * map were different therefore mutating one didn't affect the other.
   * Originally, node B was first added to both the graph and map as a child of A
   * but only updated in the map when it itself was processed. This test checks
   * that the two are the same.
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

    Mutation.Builder addTokenToB =
        Mutation.newBuilder()
            .setStartNode("B")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut);

    String error = dataGraph.mutateGraph(addTokenToB);
    Assert.assertEquals(error.length(), 0);

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

    // The original node should not be modified by the mutation (nodes are
    // immutable)
    Assert.assertTrue(gNodeB.tokenList().size() == 0);

    HashMap<String, Set<String>> tokenMapNew = dataGraph.tokenMap();
    Assert.assertTrue(tokenMapNew.containsKey("1"));
    Assert.assertTrue(tokenMapNew.get("1").contains("B"));
    Assert.assertTrue(tokenMapNew.containsKey("2"));
    Assert.assertTrue(tokenMapNew.get("2").contains("B"));
    Assert.assertTrue(tokenMapNew.containsKey("3"));
    Assert.assertTrue(tokenMapNew.get("3").contains("B"));
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

    Mutation.Builder removeTokenFromA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut);

    HashSet<String> roots = new HashSet<>();

    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    HashSet<String> setWithA1 = new HashSet<>();
    setWithA1.add("A");
    HashSet<String> setWithA2 = new HashSet<String>(setWithA1);
    HashSet<String> setWithA3 = new HashSet<String>(setWithA1);
    HashSet<String> setWithA4 = new HashSet<String>(setWithA1);
    tokenMap.put("1", setWithA1);
    tokenMap.put("2", setWithA2);
    tokenMap.put("3", setWithA3);
    tokenMap.put("4", setWithA4);

    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(removeTokenFromA);
    Assert.assertEquals(error.length(), 0);

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

    HashMap<String, Set<String>> tokenMapNew = dataGraph.tokenMap();
    Assert.assertTrue(tokenMapNew.containsKey("1"));
    Assert.assertTrue(tokenMapNew.get("1").contains("A"));
    Assert.assertTrue(tokenMapNew.containsKey("3"));
    Assert.assertTrue(tokenMapNew.get("3").contains("A"));
    Assert.assertFalse(tokenMapNew.containsKey("2"));
    Assert.assertFalse(tokenMapNew.containsKey("4"));

    // The original node should not be modified by the mutation (nodes are
    // immutable)
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

    Mutation.Builder addToA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut);

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(addToA);
    Assert.assertEquals(error, "Change node: Unrecognized token mutation UNKNOWN\n");

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

    HashMap<String, Set<String>> tokenMapNew = dataGraph.tokenMap();
    Assert.assertTrue(tokenMapNew.isEmpty());
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

    Mutation.Builder add =
        Mutation.newBuilder().setType(Mutation.Type.CHANGE_TOKEN).setTokenChange(tokenMut);

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(add);
    Assert.assertEquals(error, "Change node: Changing a non-existent node \n");

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 3);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodesMap.containsKey("A"));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);

    HashMap<String, Set<String>> tokenMapNew = dataGraph.tokenMap();
    Assert.assertEquals(0, tokenMapNew.keySet().size()); // No tokens should have been added

    Assert.assertTrue(graph.hasEdgeConnecting(gNodeA, gNodeB));
    Assert.assertTrue(graph.hasEdgeConnecting(gNodeB, gNodeC));
  }

  /** Removing a node with tokens removes it from the token map */
  @Test
  public void deleteNodeWithTokens() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    nodeA.addToken("1");
    nodeA.addToken("2");
    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());

    nodeB.addToken("1");
    gNodeB = Utility.protoNodeToGraphNode(nodeB.build());

    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeB, gNodeC);
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);
    graphNodesMap.put("C", gNodeC);

    Mutation.Builder removeA =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("A");

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();
    HashSet<String> setWithA = new HashSet<>();
    setWithA.add("A");
    HashSet<String> setWithAB = new HashSet<>();
    setWithAB.add("A");
    setWithAB.add("B");

    tokenMap.put("1", setWithAB);
    tokenMap.put("2", setWithA);

    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(removeA);
    Assert.assertEquals(error.length(), 0);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertEquals(graphNodesMap.get("C"), gNodeC);
    Assert.assertEquals(graph.inDegree(gNodeB), 0);
    Assert.assertEquals(graph.outDegree(gNodeB), 1);

    HashMap<String, Set<String>> tokenMapNew = dataGraph.tokenMap();
    // tokenMap should only have 1 token since A was deleted. The set corresponding
    // to 1 should only have one node, B.
    Assert.assertEquals(1, tokenMapNew.keySet().size());
    Assert.assertTrue(tokenMapNew.keySet().contains("1"));
    Assert.assertEquals(1, tokenMapNew.get("1").size());
    Assert.assertTrue(tokenMapNew.get("1").contains("B"));
  }

  /**
   * Removing non-existent tokens from a node modifies the mutation by removing the spurious tokens
   */
  @Test
  public void deleteNonexistentTokensFromNode() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    nodeA.addToken("1");
    nodeA.addToken("2");
    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());

    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);

    List<String> newTokens = new ArrayList<>();
    newTokens.add("2");

    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.DELETE_TOKEN)
            .addTokenName("1")
            .addTokenName("5")
            .build();

    Mutation.Builder removeTokenFromA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut);

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();

    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(removeTokenFromA);
    Assert.assertEquals(error.length(), 0);

    Set<GraphNode> graphNodes = graph.nodes();
    GraphNode newNodeA = graphNodesMap.get("A");
    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(newNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertEquals(newNodeA.tokenList(), newTokens);

    List<String> tokenChangeList = removeTokenFromA.getTokenChange().getTokenNameList();
    Assert.assertEquals(tokenChangeList.size(), 1);
    Assert.assertEquals(tokenChangeList.get(0), "1");
  }

  /** Adding already-present tokens to a node modifies the mutation by removing the extra tokens */
  @Test
  public void addExistingTokensToNode() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    nodeA.addToken("1");
    nodeA.addToken("2");
    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());

    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graphNodesMap.put("A", gNodeA);
    graphNodesMap.put("B", gNodeB);

    List<String> newTokens = new ArrayList<>();
    newTokens.add("1");
    newTokens.add("2");
    newTokens.add("5");

    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("1")
            .addTokenName("5")
            .build();

    Mutation.Builder addTokenToA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut);

    HashSet<String> roots = new HashSet<>();
    HashMap<String, Set<String>> tokenMap = new HashMap<>();

    DataGraph dataGraph = DataGraph.create(graph, graphNodesMap, roots, 0, tokenMap);

    String error = dataGraph.mutateGraph(addTokenToA);
    Assert.assertEquals(error.length(), 0);

    Set<GraphNode> graphNodes = graph.nodes();
    GraphNode newNodeA = graphNodesMap.get("A");
    Assert.assertEquals(graphNodes.size(), 2);
    Assert.assertTrue(graphNodes.contains(newNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertEquals(graphNodesMap.get("B"), gNodeB);
    Assert.assertEquals(newNodeA.tokenList(), newTokens);

    List<String> tokenChangeList = addTokenToA.getTokenChange().getTokenNameList();
    Assert.assertEquals(tokenChangeList.size(), 1);
    Assert.assertEquals(tokenChangeList.get(0), "5");
  }
}
