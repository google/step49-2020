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

import static com.google.sps.Utility.getMutationIndicesOfNode;
import static com.google.sps.Utility.findRelevantMutations;

/**
 * This file tests the following functions: - Utility.getMutationIndicesOfNode -
 * Utility.findRelevantMutations
 */
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

    ArrayList<Integer> truncatedList = getMutationIndicesOfNode("A", multiMutList);

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

    List<Integer> truncatedList = getMutationIndicesOfNode(null, multiMutList);

    Assert.assertNotNull(truncatedList); // Should not be null
    Assert.assertEquals(0, truncatedList.size());
    Assert.assertFalse(truncatedList.contains(0));
  }

  /** Returns a standard list of multimutations that is used for testing various functions */
  private List<MultiMutation> getTestMutationList() {
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
    return multiMutList;
  }

  // TESTING findRelevantMutations
  /**
   * Getting mutation indices of multiple nodes returns the union of all their individual indices
   */
  @Test
  public void getMutationsOfMultiple() {
    List<MultiMutation> multiMutList = getTestMutationList();

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();
    nodeNames.add("A");
    nodeNames.add("B");
    nodeNames.add("D");

    Set<Integer> truncatedList = findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(2, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(3));
    Assert.assertTrue(truncatedList.contains(4));
  }

  /**
   * Getting mutation indices of multiple nodes returns the union of all their individual indices
   * without duplicates
   */
  @Test
  public void getMutationsOfMultipleNoDuplicates() {
    List<MultiMutation> multiMutList = getTestMutationList();

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();
    nodeNames.add("G");
    nodeNames.add("E");

    Set<Integer> truncatedList = findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(4, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(0));
    Assert.assertTrue(truncatedList.contains(1));
    Assert.assertTrue(truncatedList.contains(2));
    Assert.assertTrue(truncatedList.contains(3));
  }

  /** Getting mutation indices of nodes not present in any mutations returns an empty list */
  @Test
  public void getMutationsOfAbsentNodes() {
    List<MultiMutation> multiMutList = getTestMutationList();

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();
    nodeNames.add("P");
    nodeNames.add("Q");

    Set<Integer> truncatedList = findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(0, truncatedList.size());
  }

  /**
   * Getting mutation indices of nodes where some don't exist just ignores the non-existent nodes
   */
  @Test
  public void getMutationsOfSomeAbsent() {
    List<MultiMutation> multiMutList = getTestMutationList();

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();
    nodeNames.add("G");
    nodeNames.add("E");
    nodeNames.add("L");

    Set<Integer> truncatedList = findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(4, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(0));
    Assert.assertTrue(truncatedList.contains(1));
    Assert.assertTrue(truncatedList.contains(2));
    Assert.assertTrue(truncatedList.contains(3));
  }

  /**
   * Getting mutation indices of nodes with an empty list of multimutations just returns a list of
   * size 0
   */
  @Test
  public void getMutationsOfMultipleEmptyMutations() {
    List<MultiMutation> multiMutList = new ArrayList<>();

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();
    nodeNames.add("G");
    nodeNames.add("E");

    Set<Integer> truncatedList = findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(0, truncatedList.size());
  }

  /** Getting mutation indices of an empty list of nodes just returns an emtpy set */
  @Test
  public void getMutationsOfEmpty() {
    List<MultiMutation> multiMutList = getTestMutationList();

    HashMap<String, List<Integer>> mutationIndicesMap = new HashMap<>();
    Set<String> nodeNames = new HashSet<>();

    Set<Integer> truncatedList = findRelevantMutations(nodeNames, mutationIndicesMap, multiMutList);

    Assert.assertEquals(0, truncatedList.size());
  }
}
