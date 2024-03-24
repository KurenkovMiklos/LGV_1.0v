import edu.uci.ics.jung.graph.event.GraphEvent;
import org.jgrapht.*;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import java.util.Set;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {


    public static void main(String[] args) {
        //Graph<Integer, DefaultEdge> graph = GraphLoader.Load();

        /*
        Set<Integer> vertices = graph.vertexSet();
        for (Integer vertex : vertices) {
            System.out.println(vertex);
        }

        Set<DefaultEdge> edges = graph.edgeSet();
        for (DefaultEdge edge : edges) {
            Integer sourceVertex = graph.getEdgeSource(edge);
            Integer targetVertex = graph.getEdgeTarget(edge);
            System.out.println(sourceVertex + " <-> " + targetVertex);
        }*/

        VisualizationWindow myFrame = new VisualizationWindow();
        myFrame.setVisible(true);
    }


}