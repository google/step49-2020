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
import java.util.List;

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

    List<Integer> truncatedList = Utility.getMutationIndicesOfToken("4", multiMutList);
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

    List<Integer> truncatedList = Utility.getMutationIndicesOfToken("10", multiMutList);
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

    List<Integer> truncatedList = Utility.getMutationIndicesOfToken(null, multiMutList);

    Assert.assertNotNull(truncatedList); // Should not be null
    Assert.assertEquals(0, truncatedList.size());
    Assert.assertFalse(truncatedList.contains(0));
  }

  // Tests merging lists together
  @Test
  public void mergeLists() {
    List<Integer> list1 = new ArrayList<>(Arrays.asList(1, 4, 7, 15));
    List<Integer> list2 = new ArrayList<>(Arrays.asList(4, 7, 12, 13, 15));
    List<Integer> list3 = new ArrayList<>(Arrays.asList(0, 6, 9));
    List<List<Integer>> lsts = new ArrayList<>();
    lsts.add(list1);
    lsts.add(list2);
    lsts.add(list3);

    // should be 0, 1, 4, 6, 7, 9, 12, 13, 15
    List<Integer> result = Utility.mergeSortedLists(lsts);
    Assert.assertNotNull(result);
    System.out.println(result);
    Assert.assertEquals(9, result.size());
  }

  @Test
  public void mergeEmpty() {
    List<Integer> list1 = new ArrayList<>();
    List<Integer> list2 = new ArrayList<>();
    List<List<Integer>> lsts = new ArrayList<>();
    lsts.add(list1);
    lsts.add(list2);
    List<Integer> result = Utility.mergeSortedLists(lsts);
    Assert.assertEquals(0, result.size());
  }
}
