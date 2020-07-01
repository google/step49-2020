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
import com.google.sps.servlets.DataServlet;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import com.google.sps.data.GraphNode;
import com.proto.GraphProtos.Graph;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.MutationList;
import com.proto.MutationProtos.TokenMutation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public final class MutationTest {

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
    gNodeD = servlet.protoNodeToGraphNode(nodeD.build());
    gNodeE = servlet.protoNodeToGraphNode(nodeE.build());
    gNodeF = servlet.protoNodeToGraphNode(nodeF.build());
  }

  /*
   * Check that a single node is correctly added to the graph
   */
  @Test
  public void addNodeBasic() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();      
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    
    Mutation addA = Mutation.newBuilder()
                        .setType(Mutation.Type.ADD_NODE)
                        .setStartNode("A")
                        .build();

    

    boolean success = servlet.mutateGraph(addA, graph, graphNodesMap);
    Assert.assertTrue(success);

    Set<GraphNode> graphNodes = graph.nodes();
    Assert.assertEquals(graphNodes.size(), 1);
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertEquals(graphNodesMap.get("A"), gNodeA);
  }

  /*
   * Check that adding duplicate nodes is not allowed
   */
  @Test
  public void noAddDuplicates() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();  
    graph.addNode(gNodeA);    
    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();
    graphNodesMap.put("A", gNodeA);
    
    Mutation addA = Mutation.newBuilder()
                        .setType(Mutation.Type.ADD_NODE)
                        .setStartNode("A")
                        .build();

    

    boolean success = servlet.mutateGraph(addA, graph, graphNodesMap);
    Assert.assertFalse(success);
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


    
    Mutation addAB = Mutation.newBuilder()
                        .setType(Mutation.Type.ADD_EDGE)
                        .setStartNode("A")
                        .setEndNode("B")
                        .build();

    

    boolean success = servlet.mutateGraph(addAB, graph, graphNodesMap);
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


    
    Mutation addAB = Mutation.newBuilder()
                        .setType(Mutation.Type.ADD_EDGE)
                        .setStartNode("A")
                        .setEndNode("C")
                        .build();

    

    boolean success = servlet.mutateGraph(addAB, graph, graphNodesMap);
    Assert.assertFalse(success);

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



    
    Mutation removeA = Mutation.newBuilder()
                        .setType(Mutation.Type.DELETE_NODE)
                        .setStartNode("A")
                        .build();

    

    boolean success = servlet.mutateGraph(removeA, graph, graphNodesMap);
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
    
    Mutation removeC = Mutation.newBuilder()
                        .setType(Mutation.Type.DELETE_NODE)
                        .setStartNode("C")
                        .build();

    boolean success = servlet.mutateGraph(removeC, graph, graphNodesMap);
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



    
    Mutation removeAB = Mutation.newBuilder()
                        .setType(Mutation.Type.DELETE_EDGE)
                        .setStartNode("A")
                        .setEndNode("B")
                        .build();

    

    boolean success = servlet.mutateGraph(removeAB, graph, graphNodesMap);
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
    
    Mutation removeAX = Mutation.newBuilder()
                        .setType(Mutation.Type.DELETE_EDGE)
                        .setStartNode("A")
                        .build();

    boolean success = servlet.mutateGraph(removeAX, graph, graphNodesMap);
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

    
    Mutation removeAC = Mutation.newBuilder()
                        .setType(Mutation.Type.DELETE_EDGE)
                        .setStartNode("A")
                        .setEndNode("C")
                        .build();

    

    boolean success = servlet.mutateGraph(removeAC, graph, graphNodesMap);
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


    TokenMutation tokenMut = TokenMutation.newBuilder()
                                .setType(TokenMutation.Type.ADD_TOKEN)
                                .addTokenName("1")
                                .addTokenName("2")
                                .addTokenName("3")
                                .build();

    
    Mutation addTokenToA = Mutation.newBuilder()
                        .setStartNode("A")
                        .setType(Mutation.Type.CHANGE_TOKEN)
                        .setTokenChange(tokenMut)
                        .build();

    

    boolean success = servlet.mutateGraph(addTokenToA, graph, graphNodesMap);
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
    Assert.assertEquals(newNodeA.tokenList(), newTokens);
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
    gNodeA = servlet.protoNodeToGraphNode(nodeA.build());

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


    TokenMutation tokenMut = TokenMutation.newBuilder()
                                .setType(TokenMutation.Type.DELETE_TOKEN)
                                .addTokenName("2")
                                .addTokenName("4")
                                .build();

    
    Mutation removeTokenFromA = Mutation.newBuilder()
                        .setStartNode("A")
                        .setType(Mutation.Type.CHANGE_TOKEN)
                        .setTokenChange(tokenMut)
                        .build();

    

    boolean success = servlet.mutateGraph(removeTokenFromA, graph, graphNodesMap);
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

    TokenMutation tokenMut = TokenMutation.newBuilder()
                                .addTokenName("2")
                                .addTokenName("4")
                                .build();

    
    Mutation addToA = Mutation.newBuilder()
                        .setStartNode("A")
                        .setType(Mutation.Type.CHANGE_TOKEN)
                        .setTokenChange(tokenMut)
                        .build();

    

    boolean success = servlet.mutateGraph(addToA, graph, graphNodesMap);
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

    TokenMutation tokenMut = TokenMutation.newBuilder()
                                .setType(TokenMutation.Type.ADD_TOKEN)
                                .addTokenName("2")
                                .addTokenName("4")
                                .build();

    
    Mutation add = Mutation.newBuilder()
                        .setType(Mutation.Type.CHANGE_TOKEN)
                        .setTokenChange(tokenMut)
                        .build();

    

    boolean success = servlet.mutateGraph(add, graph, graphNodesMap);
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