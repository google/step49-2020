package com.google.sps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

@RunWith(JUnit4.class)
public class ReachableNodesTest {
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
    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());
    gNodeB = Utility.protoNodeToGraphNode(nodeB.build());
    gNodeC = Utility.protoNodeToGraphNode(nodeC.build());
    gNodeD = Utility.protoNodeToGraphNode(nodeD.build());
    gNodeE = Utility.protoNodeToGraphNode(nodeE.build());
    gNodeF = Utility.protoNodeToGraphNode(nodeF.build());
    gNodeG = Utility.protoNodeToGraphNode(nodeG.build());
    gNodeH = Utility.protoNodeToGraphNode(nodeH.build());
  }

  /** Radius 0 should only return the node */
  @Test
  public void reachableNodesZero() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    List<String> lst = new ArrayList<>(Arrays.asList("A"));

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 0);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(1, graphNodes.size());
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertFalse(graphNodes.contains(gNodeB));
    Assert.assertFalse(graphNodes.contains(gNodeC));

    Assert.assertEquals(0, graphEdges.size());
  }

  /** Children are considered in finding reachable nodes */
  @Test
  public void entireGraphReachableAsChildren() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    List<String> lst = new ArrayList<>(Arrays.asList("A"));

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 1);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(3, graphNodes.size());
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));

    Assert.assertEquals(2, graphEdges.size());
  }

  /** Parents are considered when finding reachable nodes */
  @Test
  public void parentsReachable() {
    nodeA.addChildren("B");
    nodeB.addChildren("C");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    List<String> lst = new ArrayList<>(Arrays.asList("C"));

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 2);

    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(3, graphNodes.size());
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));

    Assert.assertEquals(2, graphEdges.size());
  }

  /** Request only gets part of the graph, parent and children both included */
  @Test
  public void partOfGraphOnly() {
    nodeA.addChildren("B");
    nodeB.addChildren("C");
    nodeC.addChildren("D");
    nodeD.addChildren("E");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());
    protoNodesMap.put("E", nodeE.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    List<String> lst = new ArrayList<>(Arrays.asList("C"));

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 1);

    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(3, graphNodes.size());
    Assert.assertFalse(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertTrue(graphNodes.contains(gNodeD));
    Assert.assertFalse(graphNodes.contains(gNodeE));

    Assert.assertEquals(2, graphEdges.size());
  }

  /** Radius is greater than the distance of any reachable node */
  @Test
  public void requestedGreaterThanMaxDepth() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    List<String> lst = new ArrayList<>(Arrays.asList("A"));

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 5);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(3, graphNodes.size());
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));

    Assert.assertEquals(2, graphEdges.size());
  }

  /** Test nodes in a different connected component are not reachable */
  @Test
  public void diffConnectedComponentNotReachable() {
    nodeA.addChildren("B");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    List<String> lst = new ArrayList<>(Arrays.asList("B"));

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 5);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(2, graphNodes.size());
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertFalse(graphNodes.contains(gNodeC));

    Assert.assertEquals(1, graphEdges.size());
  }

  /**
   * This test mirrors the example graph we have, tests that only parents of parents (and not
   * children of parents) are added.
   */
  @Test
  public void linearNodesOnly() {
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
    List<String> lst = new ArrayList<>(Arrays.asList("B"));

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 2);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(4, graphNodes.size());
    Assert.assertTrue(graphNodes.contains(gNodeA));
    Assert.assertTrue(graphNodes.contains(gNodeB));
    Assert.assertFalse(graphNodes.contains(gNodeC)); // Child of Parent, should not be in the graph
    Assert.assertTrue(graphNodes.contains(gNodeD));
    Assert.assertTrue(graphNodes.contains(gNodeG));
    Assert.assertFalse(graphNodes.contains(gNodeE));
    Assert.assertFalse(graphNodes.contains(gNodeH));

    Assert.assertEquals(3, graphEdges.size());

    // Test encapsulation, original graph isn't modified
    Assert.assertEquals(7, graph.nodes().size());
    Assert.assertEquals(6, graph.edges().size());
  }

  /** Test that with two ways to find a node, will find the shortest */
  @Test
  public void findsShorterPath() {
    // A -> B -> D -> E -> F
    // \> C ------------/> (C points to F)
    nodeA.addChildren("B");
    nodeA.addChildren("C");
    nodeB.addChildren("D");
    nodeD.addChildren("E");
    nodeE.addChildren("F");
    nodeC.addChildren("F");

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());
    protoNodesMap.put("D", nodeD.build());
    protoNodesMap.put("E", nodeE.build());
    protoNodesMap.put("F", nodeF.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    MutableGraph<GraphNode> graph = dataGraph.graph();
    List<String> lst = new ArrayList<>(Arrays.asList("F"));

    MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 2);
    Set<GraphNode> graphNodes = truncatedGraph.nodes();
    Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();

    Assert.assertEquals(5, graphNodes.size());
    Assert.assertTrue(graphNodes.contains(gNodeA)); // Found through C because it's the shorter path
    Assert.assertFalse(graphNodes.contains(gNodeB));
    Assert.assertTrue(graphNodes.contains(gNodeC));
    Assert.assertTrue(graphNodes.contains(gNodeD));
    Assert.assertTrue(graphNodes.contains(gNodeE));
    Assert.assertTrue(graphNodes.contains(gNodeF));

    Assert.assertEquals(4, graphEdges.size());

    // Encapsulation
    Assert.assertEquals(6, graph.edges().size());
  }

    /** An empty string as a node (that doesn't exist in the graph should be ignored) */
    @Test
    public void emptyStringIgnored() {
      nodeA.addChildren("B");
      nodeA.addChildren("C");
  
      HashMap<String, Node> protoNodesMap = new HashMap<>();
      protoNodesMap.put("A", nodeA.build());
      protoNodesMap.put("B", nodeB.build());
      protoNodesMap.put("C", nodeC.build());
  
      DataGraph dataGraph = DataGraph.create();
      dataGraph.graphFromProtoNodes(protoNodesMap);
      List<String> lst = new ArrayList<>(Arrays.asList("A"));
      lst.add("");
  
      MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 0);
      Set<GraphNode> graphNodes = truncatedGraph.nodes();
      Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();
  
      Assert.assertEquals(1, graphNodes.size());
      Assert.assertTrue(graphNodes.contains(gNodeA));
      Assert.assertFalse(graphNodes.contains(gNodeB));
      Assert.assertFalse(graphNodes.contains(gNodeC));
  
      Assert.assertEquals(0, graphEdges.size());
    }

    /** Multiple nodes that are valid are all in the graph, edges preserved */
    @Test
    public void multipleValidNodesAccountedFor() {
      nodeA.addChildren("B");
      nodeA.addChildren("C");
  
      HashMap<String, Node> protoNodesMap = new HashMap<>();
      protoNodesMap.put("A", nodeA.build());
      protoNodesMap.put("B", nodeB.build());
      protoNodesMap.put("C", nodeC.build());
  
      DataGraph dataGraph = DataGraph.create();
      dataGraph.graphFromProtoNodes(protoNodesMap);
      List<String> lst = new ArrayList<>(Arrays.asList("A", "B"));
  
      MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 0);
      Set<GraphNode> graphNodes = truncatedGraph.nodes();
      Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();
  
      Assert.assertEquals(2, graphNodes.size());
      Assert.assertTrue(graphNodes.contains(gNodeA));
      Assert.assertTrue(graphNodes.contains(gNodeB));
      Assert.assertFalse(graphNodes.contains(gNodeC));
  
      Assert.assertEquals(1, graphEdges.size());
    }

    /** Invalid nodes are ignored */
    @Test
    public void invalidNodesIgnored() {
      nodeA.addChildren("B");
      nodeA.addChildren("C");
  
      HashMap<String, Node> protoNodesMap = new HashMap<>();
      protoNodesMap.put("A", nodeA.build());
      protoNodesMap.put("B", nodeB.build());
      protoNodesMap.put("C", nodeC.build());
  
      DataGraph dataGraph = DataGraph.create();
      dataGraph.graphFromProtoNodes(protoNodesMap);
      List<String> lst = new ArrayList<>(Arrays.asList("A", "B", "D"));
  
      MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 0);
      Set<GraphNode> graphNodes = truncatedGraph.nodes();
      Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();
  
      Assert.assertEquals(2, graphNodes.size());
      Assert.assertTrue(graphNodes.contains(gNodeA));
      Assert.assertTrue(graphNodes.contains(gNodeB));
      Assert.assertFalse(graphNodes.contains(gNodeC));
  
      Assert.assertEquals(1, graphEdges.size());
    }

    /** Only empty string returns a graph with max depth of radius from roots */
    @Test
    public void allInvalidNodes() {
      nodeA.addChildren("B");
      nodeA.addChildren("C");
  
      HashMap<String, Node> protoNodesMap = new HashMap<>();
      protoNodesMap.put("A", nodeA.build());
      protoNodesMap.put("B", nodeB.build());
      protoNodesMap.put("C", nodeC.build());
  
      DataGraph dataGraph = DataGraph.create();
      dataGraph.graphFromProtoNodes(protoNodesMap);
      List<String> lst = new ArrayList<>(Arrays.asList(""));
  
      MutableGraph<GraphNode> truncatedGraph = dataGraph.getReachableNodes(lst, 2);
      Set<GraphNode> graphNodes = truncatedGraph.nodes();
      Set<EndpointPair<GraphNode>> graphEdges = truncatedGraph.edges();
  
      Assert.assertEquals(3, graphNodes.size());
      Assert.assertTrue(graphNodes.contains(gNodeA));
      Assert.assertTrue(graphNodes.contains(gNodeB));
      Assert.assertTrue(graphNodes.contains(gNodeC));
  
      Assert.assertEquals(2, graphEdges.size());
    }
}
