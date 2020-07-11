package com.google.sps;

import com.google.auto.value.AutoValue;
import com.google.protobuf.Struct;
import java.util.ArrayList;
import java.util.List;

/*
 * A class that stores all necessary internal data about a graph node, for example its name,
 * list of tokens (files, AST tokens etc.), metadata as well as possible other information
 * like how it has been mutated. An object of this type does not contain dependency information
 * since this is already indicated by the edges of the graph this is a node of.
 */
@AutoValue
public abstract class GraphNode {
  public static GraphNode create(String name, List<String> tokenList, Struct metadata) {
    return new AutoValue_GraphNode(name, tokenList, metadata);
  }

  // The name of the graph node
  public abstract String name();

  // The list of "tokens" such as files or AST tokens corresponding to this node
  public abstract List<String> tokenList();

  // A structured object representing the node's metadata, for eg. source code location
  public abstract Struct metadata();

  public int hashCode() {
    return name().hashCode();
  }
}
