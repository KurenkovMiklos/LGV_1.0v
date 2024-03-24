import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;

public class CircleAlgorithm extends VertexPlacementAlgorithm {

    int width;
    int height;
    Graph<Integer, DefaultEdge> graph;


    public int[] PlaceVertexes() {
        int vertexCount = graph.vertexSet().size();
        int[] coordinates = new int[vertexCount*2];

        for (int i = 0; i < vertexCount; i++) {
            double fi = 2 * Math.PI / vertexCount * i;
            double x = Math.cos(fi) * width / 2.1 + width / 2;
            double y = Math.sin(fi) * height / 2.1 + height / 2;
            coordinates[2*i] = (int)x;
            coordinates[2*i + 1] = (int)y;
        }

        return coordinates;
    }

    @Override
    public void Init(int width, int height, Graph<Integer, DefaultEdge> graph) {
        this.width = width;
        this.height = height;
        this.graph = graph;
    }

    List<String> GetAguments() {
        return null;
    }

    @Override
    void SetAguments(List<String> args) {

    }
}
