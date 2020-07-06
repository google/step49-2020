package com.google.sps;

import com.google.common.graph.*;
import com.google.sps.servlets.DataServlet;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import com.google.sps.data.GraphNode;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;
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
   * Add node mutation adds to the root
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

}