import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import javax.imageio.ImageIO;
import java.awt.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
        coordinates = VPA.PlaceVertexes();
        int maxX = 0; int maxY = 0; int minX = 2147483647; int minY = 2147483647;
        for (int i = 0; i < vertexCount; i++) {
            if (coordinates[i*2] > maxX) { maxX = coordinates[i*2];}
            if (coordinates[i*2+1] > maxY) { maxY = coordinates[i*2+1];}
            if (coordinates[i*2] < minX) { minX = coordinates[i*2];}
            if (coordinates[i*2+1] < minY) { minY = coordinates[i*2+1];}
        }

        maxX += 10; maxY += 10;
        width = maxX - minX;
        height = maxY - minY;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        for (int i = 0; i < vertexCount; i++) {
            coordinates[i*2] += (5 - minX);
            coordinates[i*2+1] += (5 - minY);
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

    public static void main(String[] args) {
        GraphDrawer Drawer = new GraphDrawer();
        Drawer.Init(2000,1000,new RandomAlgorithm());
        try {
            File outputFile = new File("graph.png");
            ImageIO.write(Drawer.Draw(), "png", outputFile);
            System.out.println("Image saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}