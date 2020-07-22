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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for functions within Utility that are used to filter graphs across nodes */
@RunWith(JUnit4.class)
public class NodeMutationFilterTest {
  // lst1 contains even number of elements
  List<Integer> lst1 = new ArrayList<>(Arrays.asList(1, 4, 7, 15));
  // lst2 contains odd number of elements
  List<Integer> lst2 = new ArrayList<>(Arrays.asList(4, 7, 12, 13, 15));

  // Following functions test the getMutationIndicesOfNode function in Utility
  /** Basic test for including mutliple relevant nodes for getMutationIndicesOfNode */
  @Test
  public void getMutationsOfBasic() {
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

    List<Integer> truncatedList = Utility.getMutationIndicesOfNode("A", multiMutList);

    Assert.assertEquals(2, truncatedList.size());
    Assert.assertTrue(truncatedList.contains(0));
    Assert.assertTrue(truncatedList.contains(1));
  }

  /** Test that a null query returns an empty list */
  @Test
  public void getMutationsOfNull() {
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
}