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
import com.google.sps.Utility;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.google.sps.DataGraph;
import com.google.sps.GraphNode;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import com.proto.MutationProtos.Mutation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GraphMutationTest {

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

  /** Only a single mutation is applied */
  @Test
  public void oneStepMutation() {

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> origGraph = dataGraph.graph();
    HashMap<String, GraphNode> origGraphNodesMap = dataGraph.graphNodesMap();
    HashSet<String> origRoots = dataGraph.roots();
    Set<GraphNode> origNodes = origGraph.nodes();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    List<Mutation> mutList = new ArrayList<>();
    mutList.add(addAB);

    DataGraph mutatedGraph = Utility.getGraphAtMutationNumber(dataGraph, dataGraph, 1, mutList);

    MutableGraph<GraphNode> newGraph = mutatedGraph.graph();
    HashMap<String, GraphNode> newGraphNodesMap = mutatedGraph.graphNodesMap();
    HashSet<String> newRoots = mutatedGraph.roots();
    Set<GraphNode> newNodes = newGraph.nodes();
    int newNum = mutatedGraph.numMutations();

    // Assert.assertFalse(origGraph.hasEdgeConnecting(gNodeA, gNodeB));

    // Assert.assertEquals(newNum, 1);
    Assert.assertEquals(newNodes.size(), 3);
    Assert.assertTrue(newNodes.contains(gNodeA));
    Assert.assertTrue(newNodes.contains(gNodeB));
    Assert.assertTrue(newNodes.contains(gNodeC));
    Assert.assertTrue(newGraph.hasEdgeConnecting(gNodeA, gNodeB));
  }

  /** Two mutations are applied */
  @Test
  public void twoStepForwardMutation() {

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> origGraph = dataGraph.graph();
    HashMap<String, GraphNode> origGraphNodesMap = dataGraph.graphNodesMap();
    HashSet<String> origRoots = dataGraph.roots();
    Set<GraphNode> origNodes = origGraph.nodes();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeC =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("C").build();
    List<Mutation> mutList = new ArrayList<>();
    mutList.add(addAB);
    mutList.add(removeC);

    DataGraph mutatedGraph = Utility.getGraphAtMutationNumber(dataGraph, dataGraph, 2, mutList);

    MutableGraph<GraphNode> newGraph = mutatedGraph.graph();
    HashMap<String, GraphNode> newGraphNodesMap = mutatedGraph.graphNodesMap();
    HashSet<String> newRoots = mutatedGraph.roots();
    Set<GraphNode> newNodes = newGraph.nodes();
    int newNum = mutatedGraph.numMutations();

    // Assert.assertFalse(origGraph.hasEdgeConnecting(gNodeA, gNodeB));
    // Assert.assertEquals(origNodes.size(), 3);

    // Assert.assertEquals(newNum, 2);
    Assert.assertEquals(newNodes.size(), 2);
    Assert.assertTrue(newNodes.contains(gNodeA));
    Assert.assertTrue(newNodes.contains(gNodeB));
    Assert.assertFalse(newNodes.contains(gNodeC));
    Assert.assertTrue(newGraph.hasEdgeConnecting(gNodeA, gNodeB));
  }

  /** Mutation Number requested exceeds the length of the mutation list */
  @Test
  public void numberRequestedTooBig() {
    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    List<Mutation> mutList = new ArrayList<>();

    DataGraph mutatedGraph = Utility.getGraphAtMutationNumber(dataGraph, dataGraph, 2, mutList);
    Assert.assertNull(mutatedGraph);
  }

  /** The current graph node is at a mutation AFTER the one requested. */
  @Test
  public void goBack() {
    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    MutableGraph<GraphNode> origGraph = dataGraph.graph();
    HashMap<String, GraphNode> origGraphNodesMap = dataGraph.graphNodesMap();
    HashSet<String> origRoots = dataGraph.roots();
    Set<GraphNode> origNodes = origGraph.nodes();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeC =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("C").build();
    List<Mutation> mutList = new ArrayList<>();
    mutList.add(addAB);
    mutList.add(removeAB);
    mutList.add(removeC);

    // Build the current graph (same graph and map)
    DataGraph dataGraphMutated = DataGraph.create(origGraph, origGraphNodesMap, origRoots, 2);

    DataGraph mutatedGraph =
        Utility.getGraphAtMutationNumber(dataGraph, dataGraphMutated, 1, mutList);

    MutableGraph<GraphNode> newGraph = mutatedGraph.graph();
    HashMap<String, GraphNode> newGraphNodesMap = mutatedGraph.graphNodesMap();
    HashSet<String> newRoots = mutatedGraph.roots();
    Set<GraphNode> newNodes = newGraph.nodes();
    int newNum = mutatedGraph.numMutations();

    Assert.assertFalse(origGraph.hasEdgeConnecting(gNodeA, gNodeB));
    Assert.assertEquals(origNodes.size(), 3);

    // Assert.assertEquals(newNum, 1);
    Assert.assertEquals(newNodes.size(), 3);
    Assert.assertTrue(newNodes.contains(gNodeA));
    Assert.assertTrue(newNodes.contains(gNodeB));
    Assert.assertTrue(newNodes.contains(gNodeC));
    Assert.assertTrue(newGraph.hasEdgeConnecting(gNodeA, gNodeB));

    Assert.assertEquals(newRoots.size(), 2);
    Assert.assertFalse(newRoots.contains("B"));
    Assert.assertTrue(newRoots.contains("A"));
    Assert.assertTrue(newRoots.contains("C"));
  }

  /** Mutation number requested exceeds the length of the mutation list */
  @Test
  public void numberRequestedTooSmall() {
    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    List<Mutation> mutList = new ArrayList<>();

    DataGraph mutatedGraph = Utility.getGraphAtMutationNumber(dataGraph, dataGraph, -2, mutList);
    Assert.assertNull(mutatedGraph);
  }
}
