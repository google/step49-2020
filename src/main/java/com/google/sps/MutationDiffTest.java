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

  GraphNode gNodeA;
  GraphNode gNodeB;
  GraphNode gNodeC;

  @Before
  public void setUp() {
    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());
    gNodeB = Utility.protoNodeToGraphNode(nodeB.build());
    gNodeC = Utility.protoNodeToGraphNode(nodeC.build());
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

    MultiMutation result = Utility.diffBetween(multiMutList, 0);
    Assert.assertEquals(addABM, result);
    result = Utility.diffBetween(multiMutList, 1);
    Assert.assertNotNull(result);
  }

  /*
   * Test that nextIndex < currIndex + 1 is rejected
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

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);
    multiMutList.add(addACM);

    MultiMutation result = Utility.diffBetween(multiMutList, 1);
    Assert.assertEquals(addACM, result);
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

    MultiMutation result = Utility.diffBetween(multiMutList, -1);
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1);
    Assert.assertNull(result);
  }

  /*
   * Tests that a diff between currIndex and currIndex + 1 just returns the
   * multi-mutation entry at currIndex - add node
   */
  @Test
  public void forwardMutationAddNode() {
    Mutation addA =
Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A").build();
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

    MultiMutation result = Utility.diffBetween(multiMutList, 0);
    Assert.assertEquals(result, addAM);
  }

  /*
   * Tests that a diff between currIndex and currIndex + 1 just returns the
   * multi-mutation entry at currIndex - add edge
   */
  @Test
  public void forwardMutationAddEdge() {
    Mutation addA =
Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A").build();
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1);
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

    MultiMutation result = Utility.diffBetween(multiMutList, 0);
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1);
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

    MultiMutation result = Utility.diffBetween(multiMutList, 1);
    Assert.assertEquals(result, deleteBM);
  }
}
