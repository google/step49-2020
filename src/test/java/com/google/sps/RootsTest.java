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
import com.google.sps.data.GraphNode;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import com.proto.MutationProtos.Mutation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RootsTest {
  DataServlet servlet;

  // Proto nodes to construct graph with
  Builder nodeA = Node.newBuilder().setName("A");
  Builder nodeB = Node.newBuilder().setName("B");
  Builder nodeC = Node.newBuilder().setName("C");

  GraphNode gNodeA;
  GraphNode gNodeB;
  GraphNode gNodeC;

  @Before
  public void setUp() {
    servlet = new DataServlet();
    gNodeA = servlet.protoNodeToGraphNode(nodeA.build());
    gNodeB = servlet.protoNodeToGraphNode(nodeB.build());
    gNodeC = servlet.protoNodeToGraphNode(nodeC.build());
  }

  /**
   * adding an edge -> is not root adding a node -> is root QUESTIONS: should I do
   * it in terms of mutations or creating my own graph
   */

  /**
   * Add nodes without edges has all nodes as roots
   */
  @Test
  public void allNodesAsRoots() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    Assert.assertEquals(roots.size(), 2);
    Assert.assertTrue(roots.contains("A"));
    Assert.assertTrue(roots.contains("B"));
  }

  /**
   * If there's an edge in the proto graph, the child will not be a root
   */
  @Test
  public void singleEdgeChildNonRoot() {
    // A has a child, B
    nodeA.addChildren("B");
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    Assert.assertEquals(roots.size(), 1);
    Assert.assertTrue(roots.contains("A"));
    Assert.assertFalse(roots.contains("B"));
  }

  /**
   * Add edge mutation changes the root
   */
  @Test
  public void mutationAddEdgeChangesRoot() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    Mutation addAB = Mutation.newBuilder().setType(Mutation.Type.ADD_EDGE).setStartNode("A").setEndNode("B").build();

    servlet.mutateGraph(addAB, graph, graphNodesMap, roots);

    Assert.assertEquals(roots.size(), 1);
    Assert.assertTrue(roots.contains("A"));
    Assert.assertFalse(roots.contains("B"));
  }

  /**
   * Add node mutation adds to the root as well
   */
  @Test
  public void mutationAddNodeChangesRoot() {
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();
    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    Mutation addB = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("B").build();
    servlet.mutateGraph(addB, graph, graphNodesMap, roots);

    Assert.assertEquals(roots.size(), 2);
    Assert.assertTrue(roots.contains("A"));
    Assert.assertTrue(roots.contains("B"));
  }

  /**
   * Removing an edge makes a node a root is reflected
   */
  @Test
  public void mutationRemoveEdgeAddsRoot() {
    // A has a child, B
    nodeA.addChildren("B");
    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);

    Mutation removeAB = Mutation.newBuilder().setType(Mutation.Type.DELETE_EDGE).setStartNode("A").setEndNode("B")
        .build();
    servlet.mutateGraph(removeAB, graph, graphNodesMap, roots);

    Assert.assertEquals(roots.size(), 2);
    Assert.assertTrue(roots.contains("A"));
    Assert.assertTrue(roots.contains("B"));
  }

  /**
   * Remhoving an edge that doesn't change the roots
   */
  @Test
  public void mutationRemoveEdgeNoChangeToRoot() {
    // A
    // _/ \_
    // B --> C
    nodeA.addChildren("B");
    nodeA.addChildren("C");
    nodeB.addChildren("C");

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

    // Before mutation
    Assert.assertEquals(roots.size(), 1);
    Assert.assertTrue(roots.contains("A"));

    Mutation removeBC = Mutation.newBuilder().setType(Mutation.Type.DELETE_EDGE).setStartNode("B").setEndNode("C")
        .build();
    servlet.mutateGraph(removeBC, graph, graphNodesMap, roots);

    // After mutation
    Assert.assertEquals(roots.size(), 1);
    Assert.assertTrue(roots.contains("A"));
  }

  /**
   * Removing a nonroot node doesn't change the roots
   */
  @Test
  public void mutationDeleteNodeNoChangeToRoot() {
    nodeA.addChildren("B");

    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());

    HashMap<String, GraphNode> graphNodesMap = new HashMap<>();

    HashSet<String> roots = new HashSet<>();

    servlet.graphFromProtoNodes(protoNodesMap, graph, graphNodesMap, roots);
    // Before mutation
    Assert.assertEquals(roots.size(), 1);
    Assert.assertTrue(roots.contains("A"));

    Mutation removeB = Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("B").build();
    servlet.mutateGraph(removeB, graph, graphNodesMap, roots);

    // After mutation
    Assert.assertEquals(roots.size(), 1);
    Assert.assertTrue(roots.contains("A"));
  }

  /**
   * Deleting the root node will make another node(s) the root
   */
  @Test
  public void mutationDeleteRootNodeChangesNode() {
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
    // Before mutation
    Assert.assertEquals(roots.size(), 1);
    Assert.assertTrue(roots.contains("A"));

    Mutation removeA = Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("A").build();
    servlet.mutateGraph(removeA, graph, graphNodesMap, roots);

    // After mutation
    Assert.assertEquals(roots.size(), 2);
    Assert.assertTrue(roots.contains("B"));
    Assert.assertTrue(roots.contains("C"));
  }
}