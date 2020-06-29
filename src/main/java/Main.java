import com.proto.GraphProtos.Graph;
import com.proto.GraphProtos.Node;
import java.io.FileOutputStream;

class Main {
  public static void main(String[] args) {
    Node.Builder node = Node.newBuilder();
    node.setName("A");
    node.addChildren("B");

    Graph.Builder graph = Graph.newBuilder();
        graph.addRootName("A");
    graph.putNodesMap("A", node);

    FileOutputStream output = new FileOutputStream("test.txt");
    graph.build().writeTo(output);
    output.close();
  }
}
