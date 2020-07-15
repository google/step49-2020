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

import com.proto.MutationProtos.Mutation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NodeMutationFilterTest {
    /** Basic test for including mutliple relevant nodes */
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
      List<Mutation> mutList = new ArrayList<>();
      mutList.add(addAB);
      mutList.add(removeAB);
      mutList.add(removeC);
      List<Mutation> truncatedList = Utility.getMutationsOfNode("A", mutList);
  
      Assert.assertEquals(2, truncatedList.size());
      Assert.assertFalse(truncatedList.contains(removeC));
    }
  
    /** Test that a null query returns all the mutations in the list */
    @Test
    public void getMutationsOfNull() {
      Mutation addAB =
          Mutation.newBuilder()
              .setType(Mutation.Type.ADD_EDGE)
              .setStartNode("A")
              .setEndNode("B")
              .build();
      List<Mutation> mutList = new ArrayList<>();
      mutList.add(addAB);
  
      List<Mutation> truncatedList = Utility.getMutationsOfNode(null, mutList);
  
      Assert.assertEquals(1, truncatedList.size());
      Assert.assertTrue(truncatedList.contains(addAB));
    }
}