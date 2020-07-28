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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.proto.MutationProtos.MultiMutation;
import com.proto.MutationProtos.Mutation;
import com.proto.MutationProtos.TokenMutation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for functions within Utility that are used to filter graphs across nodes */
@RunWith(JUnit4.class)
public class NodeMutationFilterTest {

  // Following functions test the getMutationIndicesOfNode function in Utility
  /** Basic test for including mutliple relevant nodes for getMutationIndicesOfNode */
  @Test
  public void getMutationsOfBasicName() {
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

    ArrayList<Integer> truncatedList = Utility.getMutationIndicesOfNode("A", multiMutList);

    Assert.assertEquals(2, truncatedList.size());
    Assert.assertTrue(truncatedList.get(0) == 0);
    Assert.assertTrue(truncatedList.get(1) == 1);
  }

  /** Test that a null query returns an empty list */
  @Test
  public void getMutationsOfNullName() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();

    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);

    List<Integer> truncatedList = Utility.getMutationIndicesOfNode(null, multiMutList);

    Assert.assertNotNull(truncatedList); // Should not be null
    Assert.assertEquals(0, truncatedList.size());
    Assert.assertFalse(truncatedList.contains(0));
  }

  /** Multiple mutations modity a token, check to ensure they are all found */
  @Test
  public void getMutationsOfTokenBasic() {
    TokenMutation tokenMut1 =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("2")
            .addTokenName("4")
            .build();

    TokenMutation tokenMut2 =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.DELETE_TOKEN)
            .addTokenName("2")
            .addTokenName("3")
            .build();
    TokenMutation tokenMut3 =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("4")
            .addTokenName("7")
            .build();

    Mutation changeToken1 =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut1)
            .build();
    Mutation changeToken2 =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut2)
            .build();
    Mutation changeToken3 =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut3)
            .build();

    MultiMutation mmChange1 = MultiMutation.newBuilder().addMutation(changeToken1).build();
    MultiMutation mmChange2 = MultiMutation.newBuilder().addMutation(changeToken2).build();
    MultiMutation mmChange3 = MultiMutation.newBuilder().addMutation(changeToken3).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(mmChange1);
    multiMutList.add(mmChange2);
    multiMutList.add(mmChange3);

    Set<Integer> truncatedList = Utility.getMutationIndicesOfToken("4", multiMutList);
    Assert.assertEquals(2, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(0));
    Assert.assertTrue(truncatedList.contains(2));
    Assert.assertFalse(truncatedList.contains(1));
  }

  /** Token is not found anywhere */
  @Test
  public void tokenNotMutated() {
    TokenMutation tokenMut1 =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("2")
            .addTokenName("4")
            .build();

    TokenMutation tokenMut2 =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.DELETE_TOKEN)
            .addTokenName("2")
            .addTokenName("3")
            .build();
    TokenMutation tokenMut3 =
        TokenMutation.newBuilder()
            .setType(TokenMutation.Type.ADD_TOKEN)
            .addTokenName("4")
            .addTokenName("7")
            .build();

    Mutation changeToken1 =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut1)
            .build();
    Mutation changeToken2 =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut2)
            .build();
    Mutation changeToken3 =
        Mutation.newBuilder()
            .setStartNode("A")
            .setType(Mutation.Type.CHANGE_TOKEN)
            .setTokenChange(tokenMut3)
            .build();

    MultiMutation mmChange1 = MultiMutation.newBuilder().addMutation(changeToken1).build();
    MultiMutation mmChange2 = MultiMutation.newBuilder().addMutation(changeToken2).build();
    MultiMutation mmChange3 = MultiMutation.newBuilder().addMutation(changeToken3).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(mmChange1);
    multiMutList.add(mmChange2);
    multiMutList.add(mmChange3);

    Set<Integer> truncatedList = Utility.getMutationIndicesOfToken("10", multiMutList);
    Assert.assertEquals(0, truncatedList.size());
    Assert.assertFalse(truncatedList.contains(0));
    Assert.assertFalse(truncatedList.contains(2));
    Assert.assertFalse(truncatedList.contains(1));
  }

  /** Token mutations of null return back something empty */
  @Test
  public void tokenMutationsOfNull() {
    Mutation addAB =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("A")
            .setEndNode("B")
            .build();

    MultiMutation addABM = MultiMutation.newBuilder().addMutation(addAB).build();
    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(addABM);

    Set<Integer> truncatedList = Utility.getMutationIndicesOfToken(null, multiMutList);

    Assert.assertNotNull(truncatedList); // Should not be null
    Assert.assertEquals(0, truncatedList.size());
    Assert.assertFalse(truncatedList.contains(0));
  }

  /**
   * Getting mutation indices of multiple nodes returns the union of all their individual indices in
   * sorted order
   */
  @Test
  public void getMutationsOfMultiple() {
    Mutation removeEF =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("E")
            .setEndNode("F")
            .build();
    Mutation removeF =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("F").build();

    Mutation addG = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("G").build();
    Mutation addEG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("E")
            .setEndNode("G")
            .build();

    Mutation addH = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("H").build();
    Mutation addHG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("H")
            .setEndNode("G")
            .build();

    Mutation addDG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("D")
            .setEndNode("G")
            .build();

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

    MultiMutation removeFM =
        MultiMutation.newBuilder().addMutation(removeEF).addMutation(removeF).build();
    MultiMutation addGM = MultiMutation.newBuilder().addMutation(addG).addMutation(addEG).build();
    MultiMutation addHM = MultiMutation.newBuilder().addMutation(addH).addMutation(addHG).build();
    MultiMutation addDGM = MultiMutation.newBuilder().addMutation(addDG).build();
    MultiMutation addTokenToBM = MultiMutation.newBuilder().addMutation(addTokenToB).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(removeFM);
    multiMutList.add(addGM);
    multiMutList.add(addHM);
    multiMutList.add(addDGM);
    multiMutList.add(addTokenToBM);

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();
    nodeNames.add("A");
    nodeNames.add("B");
    nodeNames.add("D");

    Set<Integer> truncatedList =
        Utility.findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(2, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(3));
    Assert.assertTrue(truncatedList.contains(4));
  }

  /**
   * Getting mutation indices of multiple nodes returns the union of all their individual indices in
   * sorted order without duplicates
   */
  @Test
  public void getMutationsOfMultipleNoDuplicates() {
    Mutation removeEF =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("E")
            .setEndNode("F")
            .build();
    Mutation removeF =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("F").build();

    Mutation addG = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("G").build();
    Mutation addEG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("E")
            .setEndNode("G")
            .build();

    Mutation addH = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("H").build();
    Mutation addHG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("H")
            .setEndNode("G")
            .build();

    Mutation addDG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("D")
            .setEndNode("G")
            .build();

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

    MultiMutation removeFM =
        MultiMutation.newBuilder().addMutation(removeEF).addMutation(removeF).build();
    MultiMutation addGM = MultiMutation.newBuilder().addMutation(addG).addMutation(addEG).build();
    MultiMutation addHM = MultiMutation.newBuilder().addMutation(addH).addMutation(addHG).build();
    MultiMutation addDGM = MultiMutation.newBuilder().addMutation(addDG).build();
    MultiMutation addTokenToBM = MultiMutation.newBuilder().addMutation(addTokenToB).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(removeFM);
    multiMutList.add(addGM);
    multiMutList.add(addHM);
    multiMutList.add(addDGM);
    multiMutList.add(addTokenToBM);

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();
    nodeNames.add("G");
    nodeNames.add("E");

    Set<Integer> truncatedList =
        Utility.findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(4, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(0));
    Assert.assertTrue(truncatedList.contains(1));
    Assert.assertTrue(truncatedList.contains(2));
    Assert.assertTrue(truncatedList.contains(3));
  }

  /**
   * Getting mutation indices of multiple nodes returns the union of all their individual indices in
   * sorted order without duplicates, ignoring empty strings
   */
  @Test
  public void getMutationsOfMultipleIgnoreEmpty() {
    Mutation removeEF =
        Mutation.newBuilder()
            .setType(Mutation.Type.DELETE_EDGE)
            .setStartNode("E")
            .setEndNode("F")
            .build();
    Mutation removeF =
        Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("F").build();

    Mutation addG = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("G").build();
    Mutation addEG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("E")
            .setEndNode("G")
            .build();

    Mutation addH = Mutation.newBuilder().setType(Mutation.Type.ADD_NODE).setStartNode("H").build();
    Mutation addHG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("H")
            .setEndNode("G")
            .build();

    Mutation addDG =
        Mutation.newBuilder()
            .setType(Mutation.Type.ADD_EDGE)
            .setStartNode("D")
            .setEndNode("G")
            .build();

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

    MultiMutation removeFM =
        MultiMutation.newBuilder().addMutation(removeEF).addMutation(removeF).build();
    MultiMutation addGM = MultiMutation.newBuilder().addMutation(addG).addMutation(addEG).build();
    MultiMutation addHM = MultiMutation.newBuilder().addMutation(addH).addMutation(addHG).build();
    MultiMutation addDGM = MultiMutation.newBuilder().addMutation(addDG).build();
    MultiMutation addTokenToBM = MultiMutation.newBuilder().addMutation(addTokenToB).build();

    List<MultiMutation> multiMutList = new ArrayList<>();
    multiMutList.add(removeFM);
    multiMutList.add(addGM);
    multiMutList.add(addHM);
    multiMutList.add(addDGM);
    multiMutList.add(addTokenToBM);

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();
    nodeNames.add("G");
    nodeNames.add("E");
    nodeNames.add("");

    Set<Integer> truncatedList =
        Utility.findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(4, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(0));
    Assert.assertTrue(truncatedList.contains(1));
    Assert.assertTrue(truncatedList.contains(2));
    Assert.assertTrue(truncatedList.contains(3));
  }
}
