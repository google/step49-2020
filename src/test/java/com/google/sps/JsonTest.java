// Copyright 2019 Google LLC
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
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.sps.servlets.DataServlet;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import com.google.sps.data.GraphNode;
import com.proto.GraphProtos.Node;
import com.proto.GraphProtos.Node.Builder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public final class JsonTest {

  DataServlet servlet;

  // Proto nodes to construct graph with
  Builder nodeA = Node.newBuilder().setName("A");
  Builder nodeB = Node.newBuilder().setName("B");
  Builder nodeC = Node.newBuilder().setName("C");
  Builder nodeD = Node.newBuilder().setName("D");
  Builder nodeE = Node.newBuilder().setName("E");
  Builder nodeF = Node.newBuilder().setName("F");

  GraphNode gNodeA;
  GraphNode gNodeB;
  GraphNode gNodeC;
  GraphNode gNodeD;
  GraphNode gNodeE;
  GraphNode gNodeF;

  Gson gson;

  String jNodeA;
  String jNodeB;
  String jNodeC;
  String jNodeD;
  String jNodeE;
  String jNodeF;

  @Before
  public void setUp() {
    servlet = new DataServlet();
    gson = new Gson();

    gNodeA = servlet.protoNodeToGraphNode(nodeA.build());
    gNodeB = servlet.protoNodeToGraphNode(nodeB.build());
    gNodeC = servlet.protoNodeToGraphNode(nodeC.build());
    gNodeD = servlet.protoNodeToGraphNode(nodeD.build());
    gNodeE = servlet.protoNodeToGraphNode(nodeE.build());
    gNodeF = servlet.protoNodeToGraphNode(nodeF.build());

    jNodeA = gson.toJson(gNodeA);
    jNodeB = gson.toJson(gNodeB);
    jNodeC = gson.toJson(gNodeC);
    jNodeD = gson.toJson(gNodeD);
    jNodeE = gson.toJson(gNodeE);
    jNodeF = gson.toJson(gNodeF);
  }

  /*
   * Tests that a graph with no edges (all roots) is correctly converted to a JSON string
   */
  @Test
  public void onlyNodesNoEdges() {
    nodeA.addToken("1");
    nodeA.addToken("2");

    nodeC.addToken("3");

    gNodeA = servlet.protoNodeToGraphNode(nodeA.build());
    gNodeC = servlet.protoNodeToGraphNode(nodeC.build());

    jNodeA = gson.toJson(gNodeA);
    jNodeC = gson.toJson(gNodeC);


    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);

    HashSet<String> roots = new HashSet<>();
    roots.add("A");
    roots.add("B");
    roots.add("C");

    String result = servlet.graphToJson(graph, roots);
    JsonElement jsonElem = JsonParser.parseString(result);
    Assert.assertTrue(jsonElem.isJsonArray());

    JsonArray arr = jsonElem.getAsJsonArray();
    Assert.assertEquals(arr.size(), 3);
    Assert.assertTrue(arr.get(0).isJsonArray());
    Assert.assertTrue(arr.get(1).isJsonArray());
    Assert.assertTrue(arr.get(2).isJsonArray());

    JsonArray actNodes = arr.get(0).getAsJsonArray();
    JsonArray actEdges = arr.get(1).getAsJsonArray();
    JsonArray actRoots = arr.get(2).getAsJsonArray();

    Assert.assertEquals(actNodes.get(0).toString(), jNodeA);
    Assert.assertEquals(actNodes.get(1).toString(), jNodeB);
    Assert.assertEquals(actNodes.get(2).toString(), jNodeC);

    Assert.assertEquals(actEdges.size(), 0);

    Assert.assertEquals(actRoots.get(0).getAsString(), "A");
    Assert.assertEquals(actRoots.get(1).getAsString(), "B");
    Assert.assertEquals(actRoots.get(2).getAsString(), "C");
  }

  /*
   * Tests that a graph with both nodes and edges is correctly converted to a JSON string
   */
  @Test
  public void nodesAndEdges() {
    nodeA.addChildren("B");
    nodeA.addChildren("C");
    nodeA.addToken("1");
    nodeA.addToken("2");

    nodeC.addToken("3");

    gNodeA = servlet.protoNodeToGraphNode(nodeA.build());
    gNodeC = servlet.protoNodeToGraphNode(nodeC.build());

    jNodeA = gson.toJson(gNodeA);
    jNodeC = gson.toJson(gNodeC);


    MutableGraph<GraphNode> graph = GraphBuilder.directed().build();
    graph.addNode(gNodeA);
    graph.addNode(gNodeB);
    graph.addNode(gNodeC);
    graph.putEdge(gNodeA, gNodeB);
    graph.putEdge(gNodeA, gNodeC);

    HashSet<String> roots = new HashSet<>();
    roots.add("A");
    roots.add("B");
    roots.add("C");

    String edgeAB = gson.toJson(EndpointPair.ordered(gNodeA, gNodeB));
    String edgeAC = gson.toJson(EndpointPair.ordered(gNodeA, gNodeC));

    String result = servlet.graphToJson(graph, roots);
    JsonElement jsonElem = JsonParser.parseString(result);
    Assert.assertTrue(jsonElem.isJsonArray());

    JsonArray arr = jsonElem.getAsJsonArray();
    Assert.assertEquals(arr.size(), 3);
    Assert.assertTrue(arr.get(0).isJsonArray());
    Assert.assertTrue(arr.get(1).isJsonArray());
    Assert.assertTrue(arr.get(2).isJsonArray());

    JsonArray actNodes = arr.get(0).getAsJsonArray();
    JsonArray actEdges = arr.get(1).getAsJsonArray();
    JsonArray actRoots = arr.get(2).getAsJsonArray();

    Assert.assertEquals(actNodes.get(0).toString(), jNodeA);
    Assert.assertEquals(actNodes.get(1).toString(), jNodeB);
    Assert.assertEquals(actNodes.get(2).toString(), jNodeC);

    Assert.assertEquals(actEdges.size(), 2);
    Assert.assertEquals(actEdges.get(0).toString(), edgeAB);
    Assert.assertEquals(actEdges.get(1).toString(), edgeAC);

    Assert.assertEquals(actRoots.get(0).getAsString(), "A");
    Assert.assertEquals(actRoots.get(1).getAsString(), "B");
    Assert.assertEquals(actRoots.get(2).getAsString(), "C");
  }
}
