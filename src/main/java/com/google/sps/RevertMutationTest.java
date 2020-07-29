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

import java.util.List;

import com.proto.MutationProtos.MultiMutation;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for functions within Utility that are used to "undo" mutations */
@RunWith(JUnit4.class)
public class RevertMutationTest {

  /** Tests whether undoing an add token mutation results in a delete token mutation */
  @Test
  public void revertAddToken() {
    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("1")
            .addTokenName("2")
            .addTokenName("3")
            .build();
    TokenMutation revertedMut = Utility.revertTokenChangeMutation(tokenMut);
    Assert.assertEquals(revertedMut.getType(), TokenMutation.Type.DELETE_TOKEN);

    List<String> revertedTokenList = revertedMut.getTokenNameList();
    Assert.assertEquals(revertedTokenList.size(), 3);
    Assert.assertEquals(revertedTokenList.get(0), "1");
    Assert.assertEquals(revertedTokenList.get(1), "2");
    Assert.assertEquals(revertedTokenList.get(2), "3");
  }

  /** Tests whether undoing an delete token mutation results in an add token mutation */
  @Test
  public void revertDeleteToken() {
    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.DELETE_TOKEN)
            .addTokenName("1")
            .addTokenName("2")
            .addTokenName("3")
            .build();
    TokenMutation revertedMut = Utility.revertTokenChangeMutation(tokenMut);
    Assert.assertEquals(revertedMut.getType(), TokenMutation.Type.ADD_TOKEN);

    List<String> revertedTokenList = revertedMut.getTokenNameList();
    Assert.assertEquals(revertedTokenList.size(), 3);
    Assert.assertEquals(revertedTokenList.get(0), "1");
    Assert.assertEquals(revertedTokenList.get(1), "2");
    Assert.assertEquals(revertedTokenList.get(2), "3");
  }

  /** Tests whether undoing an add node mutation results in a delete node mutation */
  @Test
  public void revertAddNode() {
    Mutation addA = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("A").build();

    Mutation revertedMut = Utility.revertMutation(addA);
    Assert.assertEquals(revertedMut.getType(), Mutation.Type.DELETE_NODE);
    Assert.assertEquals(revertedMut.getStartNode(), "A");
  }

  /** Tests whether undoing a delete node mutation results in an add node mutation */
  @Test
  public void revertDeleteNode() {
    Mutation deleteA =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("A").build();

    Mutation revertedMut = Utility.revertMutation(deleteA);
    Assert.assertEquals(revertedMut.getType(), Mutation.Type.ADD_NODE);
    Assert.assertEquals(revertedMut.getStartNode(), "A");
  }

  /** Tests whether undoing an add edge mutation results in a delete edge mutation */
  @Test
  public void revertAddEdge() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();

    Mutation revertedMut = Utility.revertMutation(addAB);
    Assert.assertEquals(revertedMut.getType(), Mutation.Type.DELETE_EDGE);
    Assert.assertEquals(revertedMut.getStartNode(), "A");
    Assert.assertEquals(revertedMut.getEndNode(), "B");
  }

  /** Tests whether undoing a delete edge mutation results in an add edge mutation */
  @Test
  public void revertDeleteEdge() {
    Mutation deleteAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();

    Mutation revertedMut = Utility.revertMutation(deleteAB);
    Assert.assertEquals(revertedMut.getType(), Mutation.Type.ADD_EDGE);
    Assert.assertEquals(revertedMut.getStartNode(), "A");
    Assert.assertEquals(revertedMut.getEndNode(), "B");
  }

  /**
   * Tests whether undoing a change token mutation results in a change token mutation with the
   * reverted token mutation
   */
  @Test
  public void revertChangeToken() {
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

    Mutation revertedMut = Utility.revertMutation(addTokenToA);

    Assert.assertEquals(revertedMut.getType(), Mutation.Type.CHANGE_TOKEN);
    Assert.assertEquals(revertedMut.getTokenChange(), Utility.revertTokenChangeMutation(tokenMut));
  }

  /** Tests whether undoing a multimutation undoes each contained mutation in the opposite order */
  @Test
  public void revertMultiMutation() {
    TokenMutation tokenMut =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.DELETE_TOKEN)
            .addTokenName("1")
            .addTokenName("2")
            .addTokenName("3")
            .build();
    Mutation deleteFromF =
        Mutation.newBuilder()
            .setStartNode("F")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut)
            .build();
    Mutation removeEF =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("E")
            .setEndNode("F")
            .build();
    Mutation removeGF =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("G")
            .setEndNode("F")
            .build();
    Mutation removeF =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("F").build();
    MultiMutation removeFM =
        MultiMutation.newBuilder()
            .addMutation(deleteFromF)
            .addMutation(removeEF)
            .addMutation(removeGF)
            .addMutation(removeF)
            .setReason("removing F")
            .build();

    MultiMutation revertedMultiMut = Utility.revertMultiMutation(removeFM);
    List<Mutation> mutList = revertedMultiMut.getMutationList();

    Assert.assertEquals(revertedMultiMut.getReason(), "removing F");
    Assert.assertEquals(mutList.size(), 4);
    Assert.assertEquals(mutList.get(0), Utility.revertMutation(removeF));
    Assert.assertEquals(mutList.get(1), Utility.revertMutation(removeGF));
    Assert.assertEquals(mutList.get(2), Utility.revertMutation(removeEF));
    Assert.assertEquals(mutList.get(3), Utility.revertMutation(deleteFromF));
  }
}
