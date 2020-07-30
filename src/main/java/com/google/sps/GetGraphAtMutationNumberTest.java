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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import com.proto.MutationProtos.MultiMutation;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.sps.Utility.getGraphAtMutationNumber;
import static com.google.sps.Utility.protoNodeToGraphNode;

@RunWith(JUnit4.class)
public class GetGraphAtMutationNumberTest {

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

  /** Only a single mutation is applied */
  @Test
  public void oneStepMutation() {

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    DataGraph originalCopy = dataGraph.getCopy();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);

    DataGraph mutatedGraph = getGraphAtMutationNumber(dataGraph, originalCopy, 0, multiMutList);

    MutableGraph<GraphNode> newGraph = mutatedGraph.graph();
    Set<GraphNode> newNodes = newGraph.nodes();
    int newNum = mutatedGraph.numMutations();

    Assert.assertEquals(newNum, 0);
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
    DataGraph originalCopy = dataGraph.getCopy();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeC =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("C").build();

    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    MultiMutation removeCM = MultiMutation.newBuilder().addMutation(removeC).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);
    multiMutList.add(removeCM);

    DataGraph mutatedGraph = getGraphAtMutationNumber(dataGraph, originalCopy, 1, multiMutList);

    MutableGraph<GraphNode> newGraph = mutatedGraph.graph();
    Set<GraphNode> newNodes = newGraph.nodes();
    int newNum = mutatedGraph.numMutations();

    Assert.assertEquals(newNum, 1);
    Assert.assertEquals(newNodes.size(), 2);
    Assert.assertTrue(newNodes.contains(gNodeA));
    Assert.assertTrue(newNodes.contains(gNodeB));
    Assert.assertFalse(newNodes.contains(gNodeC));
    Assert.assertTrue(newGraph.hasEdgeConnecting(gNodeA, gNodeB));
  }

  /** Two mutations are applied, one of which mutates the tokens of a node */
  @Test
  public void twoStepForwardMutationTokenMut() {

    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    DataGraph originalCopy = dataGraph.getCopy();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("2")
            .addTokenName("4")
            .build();

    Mutation addToA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut)
            .build();

    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    MultiMutation addToAM = MultiMutation.newBuilder().addMutation(addToA).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);
    multiMutList.add(addToAM);

    DataGraph mutatedGraph = getGraphAtMutationNumber(dataGraph, originalCopy, 1, multiMutList);

    MutableGraph<GraphNode> newGraph = mutatedGraph.graph();
    HashMap<String, GraphNode> newGraphNodesMap = mutatedGraph.graphNodesMap();
    Set<GraphNode> newNodes = newGraph.nodes();
    int newNum = mutatedGraph.numMutations();

    Assert.assertTrue(newGraphNodesMap.containsKey("A"));
    GraphNode newNodeA = newGraphNodesMap.get("A");
    List<String> newTokenList = new ArrayList<>();
    newTokenList.add("2");
    newTokenList.add("4");

    Assert.assertEquals(newNum, 1);
    Assert.assertEquals(newNodes.size(), 3);
    Assert.assertTrue(newNodes.contains(newNodeA));
    Assert.assertTrue(newNodes.contains(gNodeB));
    Assert.assertTrue(newNodes.contains(gNodeC));
    Assert.assertTrue(newGraph.hasEdgeConnecting(newNodeA, gNodeB));
    Assert.assertEquals(newNodeA.tokenList(), newTokenList);

    HashMap<String, Set<String>> mutatedTokenMap = mutatedGraph.tokenMap();
    Assert.assertEquals(2, mutatedTokenMap.keySet().size());
    Assert.assertTrue(mutatedTokenMap.keySet().contains("2"));
    Assert.assertTrue(mutatedTokenMap.keySet().contains("4"));

    Assert.assertEquals(1, mutatedTokenMap.get("4").size());
    Assert.assertTrue(mutatedTokenMap.get("4").contains("A"));
    Assert.assertEquals(1, mutatedTokenMap.get("2").size());
    Assert.assertTrue(mutatedTokenMap.get("2").contains("A"));

    // Make sure they're not referentially equal
    Assert.assertFalse(dataGraph.tokenMap() == originalCopy.tokenMap());
    Assert.assertFalse(mutatedTokenMap == dataGraph.tokenMap());
  }

  /**
   * Mutation Number requested exceeds the length of the mutation list, should return the graph
   * after applying all the mutations
   */
  @Test
  public void numberRequestedTooBig() {
    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);
    DataGraph dataGraphCopy = dataGraph.getCopy();

    List<MultiMutation> multiMutList = new ArrayList<>();

    DataGraph mutatedGraph = getGraphAtMutationNumber(dataGraph, dataGraphCopy, 1, multiMutList);
    MutableGraph<GraphNode> newGraph = mutatedGraph.graph();
    HashSet<String> newRoots = mutatedGraph.roots();
    Set<GraphNode> newNodes = newGraph.nodes();
    int newNum = mutatedGraph.numMutations();

    Assert.assertEquals(
        -1, newNum); // This would be the original graph since there are no mutations
    Assert.assertEquals(3, newNodes.size());
    Assert.assertEquals(3, newRoots.size());
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

    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    MultiMutation removeABM = MultiMutation.newBuilder().addMutation(removeAB).build();
    MultiMutation removeCM = MultiMutation.newBuilder().addMutation(removeC).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);
    multiMutList.add(removeABM);
    multiMutList.add(removeCM);
    HashMap<String, Set<String>> tokenMap = new HashMap<>();

    // Build the current graph (same graph and map)
    // This graph is the one after adding and removing AB but before removing C
    DataGraph dataGraphMutated =
        DataGraph.create(origGraph, origGraphNodesMap, origRoots, 2, tokenMap);

    DataGraph mutatedGraph = getGraphAtMutationNumber(dataGraph, dataGraphMutated, 0, multiMutList);

    MutableGraph<GraphNode> newGraph = mutatedGraph.graph();
    HashSet<String> newRoots = mutatedGraph.roots();
    Set<GraphNode> newNodes = newGraph.nodes();
    int newNum = mutatedGraph.numMutations();

    Assert.assertFalse(origGraph.hasEdgeConnecting(gNodeA, gNodeB));
    Assert.assertEquals(origNodes.size(), 3);

    Assert.assertEquals(newNum, 0);
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
    DataGraph dataGraphCopy = dataGraph.getCopy();

    List<MultiMutation> mutList = new ArrayList<>();

    DataGraph mutatedGraph = getGraphAtMutationNumber(dataGraph, dataGraphCopy, -2, mutList);
    Assert.assertNull(mutatedGraph);
  }

  /** Original and current graphs being referentially equal is not allowed */
  @Test
  public void originalAndCurrentNotCopies() {
    HashMap<String, Node> protoNodesMap = new HashMap<>();
    protoNodesMap.put("A", nodeA.build());
    protoNodesMap.put("B", nodeB.build());
    protoNodesMap.put("C", nodeC.build());

    DataGraph dataGraph = DataGraph.create();
    dataGraph.graphFromProtoNodes(protoNodesMap);

    List<MultiMutation> mutList = new ArrayList<>();

    Assert.assertThrows(
        IllegalArgumentException.class,
        () -> getGraphAtMutationNumber(dataGraph, dataGraph, 0, mutList));
  }
}
