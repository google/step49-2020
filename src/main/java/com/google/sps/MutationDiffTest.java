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

@RunWith(JUnit4.class)
public class MutationDiffTest {

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

  /*
   * Test that nextIndex > currIndex + 1 is rejected
   */
  @Test
  public void nonConsecutiveIndicesPos() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 0, 2);
    Assert.assertNull(result);
    result = Utility.diffBetween(multiMutList, 1, 2);
    Assert.assertNotNull(result);
  }

  /*
   * Test that nextIndex < currIndex - 1 is rejected
   */
  @Test
  public void nonConsecutiveIndicesNeg() {
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

    Mutation addAD =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("D")
            .build();
    MultiMutation addADM = MultiMutation.newBuilder().addMutation(addAD).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);
    multiMutList.add(addACM);
    multiMutList.add(addADM);

    MultiMutation result = Utility.diffBetween(multiMutList, 3, 0);
    Assert.assertNull(result);
    result = Utility.diffBetween(multiMutList, 3, 1);
    Assert.assertNull(result);
    result = Utility.diffBetween(multiMutList, 3, 2);
    Assert.assertNotNull(result);
  }

  /*
   * Test that a negative current index is rejected
   */
  @Test
  public void invalidCurrIndex() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);

    MultiMutation result = Utility.diffBetween(multiMutList, -1, 0);
    Assert.assertNull(result);
  }

  /*
   * Test that a negative next index is rejected
   */
  @Test
  public void invalidNextIndex() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);

    MultiMutation result = Utility.diffBetween(multiMutList, 0, -1);
    Assert.assertNull(result);
  }

  /*
   * Test that a current index larger than the number of mutations is rejected
   */
  @Test
  public void tooLargeCurrIndex() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);

    MultiMutation result = Utility.diffBetween(multiMutList, 2, 1);
    Assert.assertNull(result);
  }

  /*
   * Test that a next index larger than the number of mutations is rejected
   */
  @Test
  public void tooLargeNextIndex() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();
    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);

    MultiMutation result = Utility.diffBetween(multiMutList, 1, 2);
    Assert.assertNull(result);
  }

  /*
   * Tests that a diff between currIndex and currIndex + 1 just returns the
   * multi-mutation entry at currIndex - add node
   */
  @Test
  public void forwardMutationAddNode() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1, 2);
    Assert.assertEquals(result, addABM);
  }

  /*
   * Tests that a diff between currIndex and currIndex + 1 just returns the
   * multi-mutation entry at currIndex - change token
   */
  @Test
  public void forwardMutationChangeToken() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 0, 1);
    Assert.assertEquals(result, addTokenToAM);
  }

  /*
   * Tests that a diff between currIndex and currIndex + 1 just returns the
   * multi-mutation entry at currIndex - delete edge
   */
  @Test
  public void forwardMutationDeleteEdge() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1, 2);
    Assert.assertEquals(result, removeABM);
  }

  /*
   * Tests that a diff between currIndex and currIndex + 1 just returns the
   * multi-mutation entry at currIndex - delete node
   */
  @Test
  public void forwardMutationDeleteNode() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1, 2);
    Assert.assertEquals(result, deleteBM);
    // List<Mutation> mutList = result.getMutationList();

    // Assert.assertEquals(mutList.size(), 2);
    // Mutation firstMut = mutList.get(0);

    // Assert.assertEquals(firstMut, removeB);

    // Mutation secondMut = mutList.get(1);
    // Assert.assertEquals(secondMut, removeAB);
  }

  /*
   * Tests that a diff between currIndex and currIndex - 1 inverts the mutation at
   * currIndex - 1
   */
  @Test
  public void backwardMutationAddNode() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1, 0);
    List<Mutation> mutList = result.getMutationList();

    Assert.assertEquals(mutList.size(), 1);
    Mutation mut = mutList.get(0);

    Assert.assertEquals(mut.getType(), Mutation.Type.DELETE_NODE);
    Assert.assertEquals(mut.getStartNode(), "A");
    Assert.assertEquals(mut.getEndNode(), "");
    Assert.assertEquals(mut.getTokenChange(), addA.getTokenChange());
  }

  /*
   * Tests that a diff between currIndex and currIndex - 1 inverts the mutation at
   * currIndex - 1
   */
  @Test
  public void backwardMutationAddEdge() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 2, 1);
    List<Mutation> mutList = result.getMutationList();

    Assert.assertEquals(mutList.size(), 1);
    Mutation mut = mutList.get(0);

    Assert.assertEquals(mut.getType(), Mutation.Type.DELETE_EDGE);
    Assert.assertEquals(mut.getStartNode(), "A");
    Assert.assertEquals(mut.getEndNode(), "B");
    Assert.assertEquals(mut.getTokenChange(), addAB.getTokenChange());
  }

  /*
   * Tests that a diff between currIndex and currIndex - 1 inverts the mutation at
   * currIndex - 1
   */
  @Test
  public void backwardMutationDeleteEdge() {
    Mutation addA = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A").build();
    MultiMutation addAM = MultiMutation.newBuilder().addMutation(addA).build();

    Mutation removeBC =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("B")
            .setEndNode("C")
            .build();
    MultiMutation removeBCM = MultiMutation.newBuilder().addMutation(removeBC).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addAM);
    multiMutList.add(removeBCM);

    MultiMutation result = Utility.diffBetween(multiMutList, 2, 1);
    List<Mutation> mutList = result.getMutationList();

    Assert.assertEquals(mutList.size(), 1);
    Mutation mut = mutList.get(0);

    Assert.assertEquals(mut.getType(), Mutation.Type.ADD_EDGE);
    Assert.assertEquals(mut.getStartNode(), "B");
    Assert.assertEquals(mut.getEndNode(), "C");
    Assert.assertEquals(mut.getTokenChange(), removeBC.getTokenChange());
  }

  /*
   * Tests that a diff between currIndex and currIndex - 1 inverts the mutation at
   * currIndex - 1
   */
  @Test
  public void backwardMutationAddTokens() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1, 0);
    List<Mutation> mutList = result.getMutationList();

    Assert.assertEquals(mutList.size(), 1);
    Mutation mut = mutList.get(0);

    Assert.assertEquals(mut.getType(), Mutation.Type.CHANGE_TOKEN);
    Assert.assertEquals(mut.getStartNode(), "A");
    Assert.assertEquals(mut.getEndNode(), "");

    TokenMutation resultTokenMut = mut.getTokenChange();
    Assert.assertEquals(resultTokenMut.getType(), TokenMutation.Type.DELETE_TOKEN);
    Assert.assertEquals(resultTokenMut.getTokenNameList(), newTokens);
  }

  /*
   * Tests that a diff between currIndex and currIndex - 1 inverts the mutation at
   * currIndex - 1
   */
  @Test
  public void backwardMutationDeleteTokens() {
    List<String> changeTokens = new ArrayList<>();
    changeTokens.add("1");
    changeTokens.add("2");
    changeTokens.add("3");

    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.DELETE_TOKEN)
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1, 0);
    List<Mutation> mutList = result.getMutationList();

    Assert.assertEquals(mutList.size(), 1);
    Mutation mut = mutList.get(0);

    Assert.assertEquals(mut.getType(), Mutation.Type.CHANGE_TOKEN);
    Assert.assertEquals(mut.getStartNode(), "A");
    Assert.assertEquals(mut.getEndNode(), "");

    TokenMutation resultTokenMut = mut.getTokenChange();
    Assert.assertEquals(resultTokenMut.getType(), TokenMutation.Type.ADD_TOKEN);
    Assert.assertEquals(resultTokenMut.getTokenNameList(), changeTokens);
  }

  /*
   * Tests that a diff between currIndex and currIndex - 1 inverts the mutation at
   * currIndex - 1
   */
  @Test
  public void backwardMutationDeleteNode() {
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

    MultiMutation result = Utility.diffBetween(multiMutList, 2, 1);
    List<Mutation> mutList = result.getMutationList();

    Assert.assertEquals(mutList.size(), 2);
    Mutation firstMut = mutList.get(0);

    Assert.assertEquals(firstMut.getType(), Mutation.Type.ADD_NODE);
    Assert.assertEquals(firstMut.getStartNode(), "B");
    Assert.assertEquals(firstMut.getEndNode(), "");

    Mutation secondMut = mutList.get(1);

    Assert.assertEquals(secondMut.getType(), Mutation.Type.ADD_EDGE);
    Assert.assertEquals(secondMut.getStartNode(), "A");
    Assert.assertEquals(secondMut.getEndNode(), "B");
  }

  /*
   * Tests that a diff between currIndex and currIndex - 1 inverts the mutation at
   * currIndex - 1
   */
  @Test
  public void backwardMutationAddSyntheticNode() {
    List<String> tokens = new ArrayList<>();
    tokens.add("1");

    Mutation addC = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("C").build();

    TokenMutation tokenMutRem =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.DELETE_TOKEN)
            .addTokenName("1")
            .build();

    Mutation removeTokenFromA =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMutRem)
            .build();

    Mutation removeTokenFromB =
        Mutation.newBuilder()
            .setStartNode("B")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMutRem)
            .build();

    TokenMutation tokenMutAdd =
        TokenMutation.newBuilder().setType(TokenMutation.Type.ADD_TOKEN).addTokenName("1").build();

    Mutation addTokenToC =
        Mutation.newBuilder()
            .setStartNode("C")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMutAdd)
            .build();

    Mutation addCA =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("C")
            .setEndNode("A")
            .build();
    Mutation addCB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("C")
            .setEndNode("B")
            .build();

    MultiMutation synthModCM =
        MultiMutation.newBuilder()
            .addMutation(addC)
            .addMutation(removeTokenFromA)
            .addMutation(removeTokenFromB)
            .addMutation(addTokenToC)
            .addMutation(addCA)
            .addMutation(addCB)
            .setReason("extracting common information from A and B into synthetic module C")
            .build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(synthModCM);

    MultiMutation result = Utility.diffBetween(multiMutList, 1, 0);
    List<Mutation> mutList = result.getMutationList();

    Assert.assertEquals(mutList.size(), 6);
    Mutation firstMut = mutList.get(0);

    Assert.assertEquals(firstMut.getType(), Mutation.Type.DELETE_EDGE);
    Assert.assertEquals(firstMut.getStartNode(), "C");
    Assert.assertEquals(firstMut.getEndNode(), "B");

    Mutation secondMut = mutList.get(1);

    Assert.assertEquals(secondMut.getType(), Mutation.Type.DELETE_EDGE);
    Assert.assertEquals(secondMut.getStartNode(), "C");
    Assert.assertEquals(secondMut.getEndNode(), "A");

    Mutation thirdMut = mutList.get(2);

    Assert.assertEquals(thirdMut.getType(), Mutation.Type.CHANGE_TOKEN);
    Assert.assertEquals(thirdMut.getStartNode(), "C");
    Assert.assertEquals(thirdMut.getEndNode(), "");

    TokenMutation resultTokenMut = thirdMut.getTokenChange();
    Assert.assertEquals(resultTokenMut.getType(), TokenMutation.Type.DELETE_TOKEN);
    Assert.assertEquals(resultTokenMut.getTokenNameList(), tokens);

    Mutation fourthMut = mutList.get(3);

    Assert.assertEquals(fourthMut.getType(), Mutation.Type.CHANGE_TOKEN);
    Assert.assertEquals(fourthMut.getStartNode(), "B");
    Assert.assertEquals(fourthMut.getEndNode(), "");

    resultTokenMut = fourthMut.getTokenChange();
    Assert.assertEquals(resultTokenMut.getType(), TokenMutation.Type.ADD_TOKEN);
    Assert.assertEquals(resultTokenMut.getTokenNameList(), tokens);

    Mutation fifthMut = mutList.get(4);

    Assert.assertEquals(fifthMut.getType(), Mutation.Type.CHANGE_TOKEN);
    Assert.assertEquals(fifthMut.getStartNode(), "A");
    Assert.assertEquals(fifthMut.getEndNode(), "");

    resultTokenMut = fifthMut.getTokenChange();
    Assert.assertEquals(resultTokenMut.getType(), TokenMutation.Type.ADD_TOKEN);
    Assert.assertEquals(resultTokenMut.getTokenNameList(), tokens);

    Mutation sixthMut = mutList.get(5);

    Assert.assertEquals(sixthMut.getType(), Mutation.Type.DELETE_NODE);
    Assert.assertEquals(sixthMut.getStartNode(), "C");
    Assert.assertEquals(sixthMut.getEndNode(), "");
  }
}
