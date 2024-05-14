import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;
import java.util.Random;

public class RandomAlgorithm extends VertexPlacementAlgorithm {

    int width;
    int height;
    Graph<Integer, DefaultEdge> graph;


    public int[] PlaceVertexes() {
        int vertexCount = graph.vertexSet().size();
        int[] coordinates = new int[vertexCount*2];
        Random rand = new Random();

        for (int i = 0; i < vertexCount; i++) {
            coordinates[2*i] = rand.nextInt(width);
            coordinates[2*i + 1] = rand.nextInt(height);
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
    void SetAguments(List<String> args) {}
}
