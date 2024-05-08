import edu.uci.ics.jung.graph.event.GraphEvent;
import org.jgrapht.*;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import javax.swing.*;
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

        VisualizationWindow myFrame = VisualizationWindow.getInstance();
        myFrame.setVisible(true);


        /*JFrame progressDialog = new JFrame();
        ProgressMonitor progressMonitor = new ProgressMonitor(progressDialog, "Running ForceAtlas2:", "", 0, 100);
        progressDialog.setSize(50, 40);
        progressDialog.setAlwaysOnTop(true);
        progressDialog.setUndecorated(true);
        progressDialog.setVisible(true);



        for (int i = 0; i < 10000000; i++) {
            progressMonitor.setProgress(i/100000);
            System.out.println(i);
        }

        progressDialog.dispose();*/

    }


}