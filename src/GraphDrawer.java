import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.awt.*;

import java.awt.image.BufferedImage;
import java.util.Set;

class GraphDrawer{
    VertexPlacementAlgorithm VPA;
    Graph<Integer, DefaultEdge> graph;
    int vertexCount;
    int width;
    int height;
    int[] coordinates; //(x = coordinates[2i], y = coordinates[2i + 1])

    public void Init(int width, int height, VertexPlacementAlgorithm VPA){
        this.width = width;
        this.height = height;
        this.VPA = VPA;

        graph = GraphLoader.Load();
        vertexCount = graph.vertexSet().size();

        VPA.Init(width, height, graph);
    }
    protected BufferedImage Draw(){
        BufferedImage img = new BufferedImage(width*5, height*5, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        coordinates = VPA.PlaceVertexes();
        for (int i = 0; i < vertexCount; i++) {
            coordinates[i*2] += width*2;
            coordinates[i*2+1] += height*2;
        }


        //System.out.println(vertexCount);

        BasicStroke edgeStroke = new BasicStroke(2);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(edgeStroke);
        Set<DefaultEdge> edges = graph.edgeSet();
        for (DefaultEdge edge : edges) {
            int sourceVertex = graph.getEdgeSource(edge);
            int targetVertex = graph.getEdgeTarget(edge);
            g2d.drawLine(coordinates[2*sourceVertex - 2], coordinates[2*sourceVertex - 1],coordinates[2*targetVertex - 2], coordinates[2*targetVertex - 1]);
        }

        g2d.setColor(Color.RED);

        for (int i = 0; i < vertexCount; i++) {
            g2d.fillOval(coordinates[2*i] - 5, coordinates[2*i + 1] - 5, 10, 10);
        }


        return img;
    }
}