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

import com.proto.MutationProtos.Mutation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for functions within Utility that are used to filter graphs across nodes
 */
@RunWith(JUnit4.class)
public class NodeMutationFilterTest {
  // lst1 contains even number of elements
  List<Integer> lst1 = new ArrayList<>(Arrays.asList(1, 4, 7, 15));
  // lst2 contains odd number of elements
  List<Integer> lst2 = new ArrayList<>(Arrays.asList(4, 7, 12, 13, 15));

  // Following functions test the getMutationIndicesOfNode function in Utility
  /**
   * Basic test for including mutliple relevant nodes for getMutationIndicesOfNode
   */
  @Test
  public void getMutationsOfBasic() {
    Mutation addAB = Mutation.newBuilder().setType(Mutation.Type.ADD_EDGE).setStartNode("A").setEndNode("B").build();
    Mutation removeAB = Mutation.newBuilder().setType(Mutation.Type.DELETE_EDGE).setStartNode("A").setEndNode("B")
        .build();
    Mutation removeC = Mutation.newBuilder().setType(Mutation.Type.DELETE_NODE).setStartNode("C").build();
    List<Mutation> mutList = new ArrayList<>();
    mutList.add(addAB);
    mutList.add(removeAB);
    mutList.add(removeC);
    List<Integer> truncatedList = Utility.getMutationIndicesOfNode("A", mutList);

    Assert.assertEquals(2, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(0));
    Assert.assertTrue(truncatedList.contains(1));
    Assert.assertFalse(truncatedList.contains(2));
  }

  /** Test that a null query returns an empty list */
  @Test
  public void getMutationsOfNull() {
    Mutation addAB = Mutation.newBuilder().setType(Mutation.Type.ADD_EDGE).setStartNode("A").setEndNode("B").build();
    List<Mutation> mutList = new ArrayList<>();
    mutList.add(addAB);

    List<Integer> truncatedList = Utility.getMutationIndicesOfNode(null, mutList);

    Assert.assertNotNull(truncatedList); // Should not be null
    Assert.assertEquals(0, truncatedList.size());
    Assert.assertFalse(truncatedList.contains(0));
  }

  // The following test cases are for the getNextGreatestNumIndex function in
  // Utility.
  // Each will be on an list of even length and a list of odd length.
  // The comment will apply to the two immediate test cases under it!

  /** If no elements are greater, you get -1. */
  @Test
  public void testNextGreatestNumNoneExistEven() {
    int ans = Utility.getNextGreatestNumIndex(lst1, 20);
    Assert.assertEquals(-1, ans);
  }

  @Test
  public void testNextGreatestNumNoneExistOdd() {
    int ans = Utility.getNextGreatestNumIndex(lst1, 20);
    Assert.assertEquals(-1, ans);
  }

  /** Ensures function gets the index rather than the actual value */
  @Test
  public void testIndexNotValueEven() {
    int ans = Utility.getNextGreatestNumIndex(lst1, 8);
    Assert.assertEquals(3, ans);
  }

  @Test
  public void testIndexNotValueOdd() {
    int ans = Utility.getNextGreatestNumIndex(lst2, 8);
    Assert.assertEquals(2, ans);
  }

  /**
   * When the tgt value does exist in the middle, value found must get strictly
   * greater (value's index is returned)
   */
  @Test
  public void testInnerExistsEven() {
    int ans = Utility.getNextGreatestNumIndex(lst1, 4);
    Assert.assertEquals(2, ans);
  }

  @Test
  public void testInnerExistsOdd() {
    int ans = Utility.getNextGreatestNumIndex(lst2, 12);
    Assert.assertEquals(3, ans);
  }

  /**
   * When tgt value does not exist (DNE) in the list, value is just the immediate
   * greater one. This tests a middle index is returned correctly.
   */
  @Test
  public void testInnerDNEEven() {
    int ans = Utility.getNextGreatestNumIndex(lst1, 1);
    Assert.assertEquals(1, ans);
  }

  @Test
  public void testInnerDNEOdd() {
    int ans = Utility.getNextGreatestNumIndex(lst2, 9);
    Assert.assertEquals(2, ans);
  }

  /** Test the number must be GREATER and NOT equal (edge case, at the end) */
  @Test
  public void testStrictlyGreaterEven() {
    int ans = Utility.getNextGreatestNumIndex(lst1, 15);
    Assert.assertEquals(-1, ans);
  }

  @Test
  public void testStrictlyGreaterOdd() {
    int ans2 = Utility.getNextGreatestNumIndex(lst2, 15);
    Assert.assertEquals(-1, ans2);
  }

  /** Test when target value is the smallest */
  @Test
  public void testIsSmallestEven() {
    int ans = Utility.getNextGreatestNumIndex(lst1, 0);
    Assert.assertEquals(0, ans);
  }

  @Test
  public void testIsSmallestOdd() {
    int ans2 = Utility.getNextGreatestNumIndex(lst2, 0);
    Assert.assertEquals(0, ans2);
  }
}
