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

import com.google.common.graph.*;
import com.google.gson.Gson;
import java.util.HashSet;
import com.google.sps.data.GraphNode;
import com.google.sps.data.Utility;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Since we don't manually build the JSON, we can just check all of the fields in the JSON are
 * present.
 */
@RunWith(JUnit4.class)
public final class JsonTest {

  // Proto nodes to construct graph with
  Builder nodeA = Node.newBuilder().setName("A");
  Builder nodeB = Node.newBuilder().setName("B");
  Builder nodeC = Node.newBuilder().setName("C");

  GraphNode gNodeA;
  GraphNode gNodeB;
  GraphNode gNodeC;

  Gson gson;

  String jNodeA;
  String jNodeB;
  String jNodeC;

  @Before
  public void setUp() {
    gson = new Gson();

    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());
    gNodeB = Utility.protoNodeToGraphNode(nodeB.build());
    gNodeC = Utility.protoNodeToGraphNode(nodeC.build());

    jNodeA = gson.toJson(gNodeA);
    jNodeB = gson.toJson(gNodeB);
    jNodeC = gson.toJson(gNodeC);
  }

  /*
   * Tests that a graph with both nodes and edges is correctly converted to a JSON
   * string
   */
  @Test
  public void nodesAndEdges() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");
    nodeA.addToken("1");
    nodeA.addToken("2");

    nodeC.addToken("3");

    gNodeA = Utility.protoNodeToGraphNode(nodeA.build());
    gNodeC = Utility.protoNodeToGraphNode(nodeC.build());

    jNodeA = gson.toJson(gNodeA);
    jNodeC = gson.toJson(gNodeC);

    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeA, gNodeC);

    String result = Utility.graphToJson(graph);
    JSONObject jsonObject = new JSONObject(result);

    Assert.assertEquals(jsonObject.length(), 2);

    JSONArray elements = jsonObject.names();
    Assert.assertEquals(elements.length(), 2);

    Assert.assertTrue(jsonObject.has("nodes"));
    Assert.assertTrue(jsonObject.has("edges"));
  }
}
