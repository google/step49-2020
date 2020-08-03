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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

import static com.google.sps.Utility.protoNodeToGraphNode;
import static com.google.sps.Utility.getMultiMutationAtIndex;
import static com.google.sps.Utility.filterMultiMutationByNodes;

/**
 * This file tests the following functions: - Utility.getMultiMutationAtIndex -
 * Utility.filterMultiMutationByNodes
 */
@RunWith(JUnit4.class)
public class MutationDiffTest {

  // Proto nodes to construct graph with
  Builder nodeA = Node.newBuilder().setName("A");
  Builder nodeB = Node.newBuilder().setName("B");
  Builder nodeC = Node.newBuilder().setName("C");

  GraphNode gNodeA;
  GraphNode gNodeB;
  GraphNode gNodeC;

  @Before
  public void setUp() {
    gNodeA = protoNodeToGraphNode(nodeA.build());
    gNodeB = protoNodeToGraphNode(nodeB.build());
    gNodeC = protoNodeToGraphNode(nodeC.build());
  }

  /*
   * Test that index > multiMutList.size() - 1 is rejected
   */
  @Test
  public void outOfBoundsLarge() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();

    Mutation addAC =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("C")
            .build();
    MultiMutation addACM = MultiMutation.newBuilder().addMutation(addAC).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);
    multiMutList.add(addACM);

    MultiMutation result = getMultiMutationAtIndex(multiMutList, 2);
    Assert.assertNull(result);
    result = getMultiMutationAtIndex(multiMutList, 1);
    Assert.assertNotNull(result);
  }

  /*
   * Test that index < 0 is rejected
   */
  @Test
  public void outOfBoundsSmall() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();

    Mutation addAC =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("C")
            .build();
    MultiMutation addACM = MultiMutation.newBuilder().addMutation(addAC).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);
    multiMutList.add(addACM);

    MultiMutation result = getMultiMutationAtIndex(multiMutList, -1);
    Assert.assertNull(result);
    result = getMultiMutationAtIndex(multiMutList, 0);
    Assert.assertNotNull(result);
  }

  /*
   * Tests that calling the function on a valid index just returns the
   * multi-mutation entry at index - add node
   */
  @Test
  public void addNode() {
    Mutation addA = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A").build();
    MultiMutation addAM = MultiMutation.newBuilder().addMutation(addA).build();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addAM);
    multiMutList.add(addABM);

    MultiMutation result = getMultiMutationAtIndex(multiMutList, 0);
    Assert.assertEquals(result, addAM);
  }

  /*
   * Tests that calling the function on a valid index just returns the
   * multi-mutation entry at index - add edge
   */
  @Test
  public void addEdge() {
    Mutation addA = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A").build();
    MultiMutation addAM = MultiMutation.newBuilder().addMutation(addA).build();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addAM);
    multiMutList.add(addABM);

    MultiMutation result = getMultiMutationAtIndex(multiMutList, 1);
    Assert.assertEquals(result, addABM);
  }

  /*
   * Tests that calling the function on a valid index just returns the
   * multi-mutation entry at index - change token
   */
  @Test
  public void changeToken() {
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
    MultiMutation addTokenToAM = MultiMutation.newBuilder().addMutation(addTokenToA).build();

    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addTokenToAM);
    multiMutList.add(addABM);

    MultiMutation result = getMultiMutationAtIndex(multiMutList, 0);
    Assert.assertEquals(result, addTokenToAM);
  }

  /*
   * Tests that calling the function on a valid index just returns the
   * multi-mutation entry at index - delete edge
   */
  @Test
  public void deleteEdge() {
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
    MultiMutation addTokenToAM = MultiMutation.newBuilder().addMutation(addTokenToA).build();

    Mutation removeAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation removeABM = MultiMutation.newBuilder().addMutation(removeAB).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addTokenToAM);
    multiMutList.add(removeABM);

    MultiMutation result = getMultiMutationAtIndex(multiMutList, 1);
    Assert.assertEquals(result, removeABM);
  }

  /*
   * Tests that calling the function on a valid index just returns the
   * multi-mutation entry at index - delete node
   */
  @Test
  public void deleteNode() {
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
    MultiMutation addTokenToAM = MultiMutation.newBuilder().addMutation(addTokenToA).build();

    Mutation removeAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeB =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("B").build();
    MultiMutation deleteBM =
        MultiMutation.newBuilder()
            .addMutation(removeAB)
            .addMutation(removeB)
            .setReason("deleting node B")
            .build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addTokenToAM);
    multiMutList.add(deleteBM);

    MultiMutation result = getMultiMutationAtIndex(multiMutList, 1);
    Assert.assertEquals(result, deleteBM);
  }

  /** Tests that an empty filter on a multimutation returns the whole multimutation back */
  @Test
  public void noFilter() {
    Mutation removeAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeB =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("B").build();
    MultiMutation deleteBM =
        MultiMutation.newBuilder()
            .addMutation(removeAB)
            .addMutation(removeB)
            .setReason("deleting node B")
            .build();

    MultiMutation filteredMultiMut = filterMultiMutationByNodes(deleteBM, new HashSet<>());
    Assert.assertEquals(filteredMultiMut, deleteBM);
  }

  /*
   * Tests that a filter on a null multimutation returns null
   */
  @Test
  public void filterNullMultiMut() {
    HashSet<String> nodeNames = new HashSet<>();
    nodeNames.add("A");

    MultiMutation filteredMultiMut = filterMultiMutationByNodes(null, nodeNames);
    Assert.assertNull(filteredMultiMut);
  }

  /*
   * Tests that a filter correctly removes irrelevant mutations from a
   * multimutation
   */
  @Test
  public void filterMultiMut() {
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

    Mutation removeAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeB =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("B").build();
    MultiMutation originalMultiMut =
        MultiMutation.newBuilder()
            .addMutation(addTokenToA)
            .addMutation(removeAB)
            .addMutation(removeB)
            .setReason("adding token to A and deleting node B")
            .build();

    HashSet<String> nodeNames = new HashSet<>();
    nodeNames.add("A");

    MultiMutation filteredMultiMut = filterMultiMutationByNodes(originalMultiMut, nodeNames);
    Assert.assertNotNull(filteredMultiMut);

    List<Mutation> filteredMutList = filteredMultiMut.getMutationList();
    Assert.assertEquals(filteredMutList.size(), 1);
    Assert.assertEquals(filteredMutList.get(0), addTokenToA);
  }

  /*
   * Tests that a filter correctly removes irrelevant mutations from a
   * multimutation when the filtered node has been deleted as one of the
   * mutations
   */
  @Test
  public void filterMultiMutDeletedNode() {
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

    Mutation removeAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeB =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("B").build();
    MultiMutation originalMultiMut =
        MultiMutation.newBuilder()
            .addMutation(addTokenToA)
            .addMutation(removeAB)
            .addMutation(removeB)
            .setReason("adding token to A and deleting node B")
            .build();

    HashSet<String> nodeNames = new HashSet<>();
    nodeNames.add("B");

    MultiMutation filteredMultiMut = filterMultiMutationByNodes(originalMultiMut, nodeNames);
    Assert.assertNotNull(filteredMultiMut);

    List<Mutation> filteredMutList = filteredMultiMut.getMutationList();
    Assert.assertEquals(filteredMutList.size(), 1);
    Assert.assertEquals(filteredMutList.get(0), removeB);
  }

  /*
   * Tests that a filter correctly removes irrelevant mutations from a
   * multimutation when there are multiple nodes to filter by
   */
  @Test
  public void filterByMultipleNodes() {
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

    Mutation removeAC =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("C")
            .build();
    Mutation removeB =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("B").build();
    Mutation removeC =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("C").build();
    MultiMutation originalMultiMut =
        MultiMutation.newBuilder()
            .addMutation(addTokenToA)
            .addMutation(removeAC)
            .addMutation(removeB)
            .addMutation(removeC)
            .setReason("adding token to A and deleting node B")
            .build();

    HashSet<String> nodeNames = new HashSet<>();
    nodeNames.add("A");
    nodeNames.add("C");

    MultiMutation filteredMultiMut = filterMultiMutationByNodes(originalMultiMut, nodeNames);
    Assert.assertNotNull(filteredMultiMut);

    List<Mutation> filteredMutList = filteredMultiMut.getMutationList();
    Assert.assertEquals(filteredMutList.size(), 3);
    Assert.assertEquals(filteredMutList.get(0), addTokenToA);
    Assert.assertEquals(filteredMutList.get(1), removeAC);
    Assert.assertEquals(filteredMutList.get(2), removeC);
  }

  /*
   * Tests that a filter correctly keeps mutations of the node before its deletion
   */
  @Test
  public void filterMultiMutDeletedNodePreviousKept() {
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

    Mutation removeAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    Mutation removeB =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("B").build();
    MultiMutation originalMultiMut =
        MultiMutation.newBuilder()
            .addMutation(addTokenToB)
            .addMutation(removeAB)
            .addMutation(removeB)
            .setReason("adding token to A and deleting node B")
            .build();

    HashSet<String> nodeNames = new HashSet<>();
    nodeNames.add("B");

    MultiMutation filteredMultiMut = filterMultiMutationByNodes(originalMultiMut, nodeNames);
    Assert.assertNotNull(filteredMultiMut);

    List<Mutation> filteredMutList = filteredMultiMut.getMutationList();
    Assert.assertEquals(filteredMutList.size(), 2);
    Assert.assertEquals(filteredMutList.get(0), addTokenToB);
    Assert.assertEquals(filteredMutList.get(1), removeB);
  }
}
