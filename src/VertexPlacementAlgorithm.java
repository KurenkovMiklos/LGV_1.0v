import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import javax.swing.*;
import java.util.List;

abstract class  VertexPlacementAlgorithm /*extends SwingWorker<Void, Void>*/ {
    public abstract void Init(int width, int height, Graph<Integer, DefaultEdge> graph);
    abstract int[] PlaceVertexes();
    abstract List<String> GetAguments();
    abstract void SetAguments(List<String> args);
}
