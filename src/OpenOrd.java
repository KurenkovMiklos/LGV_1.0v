import ch.qos.logback.core.joran.conditional.ThenAction;
import org.apache.commons.lang3.ArrayUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OpenOrd extends VertexPlacementAlgorithm{
    Random rand = new Random();
    int temperature = 2000;
    double attraction = 10;
    double damping_mult = 1.0;
    double edgeCuttingParameter = 0;
    double longestEdge = 0;
    double cut_off_length = 0;
    double min_cut_off_length = 0;
    double cut_rate = 0;

    double max_cluster_distance = 50;
    int multi_level_recursion_level = 0;

    double min_edges = 100;

    private int vertexCount;
    private Graph<Integer, DefaultEdge> graph;
    private int width = 0, height = 0;
    private int loopNum = 100;
    private enum STAGE {LIQUID, EXPANSION, COOLDOWN, CRUNCH, SIMMER, DONE};     private STAGE stage = STAGE.LIQUID;
    int[] startCoordinates;
    int[] coordinates;

    int[][] density;
    List<int[]>edges;

    int[] CoarsenedMap; //maps Vertex to Clouster

    int[][] Clousters; //count, x, y

    int[] ClousterVertexesIDs;

    int[] coarsenedCoordinates;
    
    int threadCount = 3;


    @Override
    public void Init(int width, int height, Graph<Integer, DefaultEdge> graph) {
        this.graph = graph;
        this.width = width;
        this.height = height;
        vertexCount = graph.vertexSet().size();
        edges = new ArrayList<int[]>();

        for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {
            Set<DefaultEdge> currentEdges = graph.edgesOf(currentVertex);
            edges.add(new int[currentEdges.size()]);

            int i = 0;
            for (DefaultEdge edge : currentEdges) {
                int sourceVertex = graph.getEdgeSource(edge);
                int targetVertex = graph.getEdgeTarget(edge);
                int otherVertex = sourceVertex == currentVertex ? targetVertex : sourceVertex;
                edges.get(currentVertex-1)[i] = otherVertex;
                i++;
            }
        }
        RandomAlgorithm ca = new RandomAlgorithm();
        ca.Init(width, height, graph);
        startCoordinates = ca.PlaceVertexes();

        density = new int[width/50 + 1][height/50 + 1];
        CoarsenedMap = new int[vertexCount];
        Clousters = new int[vertexCount][3];
    }

    @Override
    int[] PlaceVertexes() {
        coordinates = startCoordinates.clone();

        UpdateDensity();
        /*for (int i = 0; i < density.length; i++) {
            for (int j = 0; j < density[i].length; j++) {
                System.out.print(density[i][j] + " ");
            }
            System.out.println();
        }*/

        int[] NextStageSwitch = new int[] {0, loopNum/4, loopNum/2, 3*(loopNum/4), (int) (loopNum*0.85), loopNum};


        System.out.println("LIQUID");
        stage = STAGE.LIQUID; InitParameters(); Draw();
        for (int loopCounter = NextStageSwitch[0]; loopCounter < NextStageSwitch[1]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

        System.out.println("EXPANSION");
        stage = STAGE.EXPANSION; UpdateLongestEdge(); InitParameters(); Recenter(); Draw();
        for (int loopCounter = NextStageSwitch[1]; loopCounter < NextStageSwitch[2]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

        System.out.println("COOLDOWN");
        stage = STAGE.COOLDOWN; RemoveCutEdges(); UpdateLongestEdge(); Recenter(); InitParameters(); Draw();
        for (int loopCounter = NextStageSwitch[2]; loopCounter < NextStageSwitch[3]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

        System.out.println("CRUNCH");
        stage = STAGE.CRUNCH; RemoveCutEdges(); InitParameters(); Recenter(); Draw();
        for (int loopCounter = NextStageSwitch[3]; loopCounter < NextStageSwitch[4]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

        System.out.println("SIMMER");
        stage = STAGE.SIMMER; InitParameters(); Draw();
        for (int loopCounter = NextStageSwitch[4]; loopCounter < NextStageSwitch[5]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

        System.out.println("DONE");
        stage = STAGE.DONE;
        Draw();

        if (multi_level_recursion_level > 0){
            Graph<Integer, DefaultEdge> coarsenedGraph = Coarsen();
            //System.out.println(coarsenedGraph.edgeSet());

            OpenOrd nextLevel = new OpenOrd();
            nextLevel.Init(width, height, coarsenedGraph);
            nextLevel.SetRecursion(multi_level_recursion_level - 1, max_cluster_distance * 2);
            nextLevel.SetCoordinates(coarsenedCoordinates);
            coarsenedCoordinates = nextLevel.PlaceVertexes();

            Refine();

            System.out.println("LIQUID");
            stage = STAGE.LIQUID; InitParameters();
            for (int loopCounter = NextStageSwitch[0]; loopCounter < NextStageSwitch[1]; loopCounter++) {VertexMovementLoop(); ChangeParameters();}

            System.out.println("EXPANSION");
            stage = STAGE.EXPANSION; UpdateLongestEdge(); InitParameters();
            for (int loopCounter = NextStageSwitch[1]; loopCounter < NextStageSwitch[2]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

            System.out.println("COOLDOWN");
            stage = STAGE.COOLDOWN; RemoveCutEdges(); UpdateLongestEdge(); InitParameters();
            for (int loopCounter = NextStageSwitch[2]; loopCounter < NextStageSwitch[3]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

            System.out.println("CRUNCH");
            stage = STAGE.CRUNCH; RemoveCutEdges(); InitParameters();
            for (int loopCounter = NextStageSwitch[3]; loopCounter < NextStageSwitch[4]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

            System.out.println("SIMMER");
            stage = STAGE.SIMMER; InitParameters();
            for (int loopCounter = NextStageSwitch[4]; loopCounter < NextStageSwitch[5]; loopCounter++) { VertexMovementLoop(); ChangeParameters();}

            System.out.println("DONE");
            stage = STAGE.DONE;


        }



        return coordinates;
    }

    private class VertexMovementLoopThread extends Thread {
        private final ReentrantLock coordLock = new ReentrantLock();
        Random Trand = new Random();

        int startVertex;
        int endVertex;

        STAGE Tstage;
        double Tdamping_mult;
        double Tmin_edges;
        double Tcut_off_length;
        int Twidth;
        int Theight;
        double Ttemperature;

        VertexMovementLoopThread(int s, int e){
            super();

            startVertex = s;
            endVertex = e;

            Tstage = stage;
            Tdamping_mult = damping_mult;
            Tmin_edges = min_edges;
            Tcut_off_length = cut_off_length;
            Twidth = width;
            Theight = height;
            Ttemperature = temperature;
        }

        public void run() {
            Tdamping_mult = damping_mult;
            Tmin_edges = min_edges;
            Tcut_off_length = cut_off_length;

            Iterator<int[]> edgeArrayIterator = edges.iterator();
            for (int currentVertex = startVertex; currentVertex <= endVertex; currentVertex++) {

                int x = 0; int y = 0;
                coordLock.lock();
                try {
                    x = coordinates[currentVertex * 2 - 2];
                    y = coordinates[currentVertex * 2 - 1];
                } finally {
                    coordLock.unlock();
                }


                int centralJumpX = 0;
                int centralJumpY = 0;
                double centralJumpEnergy = 0;
                int randomJumpX = 0;
                int randomJumpY = 0;
                double randomJumpEnergy = 0;
                double NoJumpEnergy = 0;

                int[] neighbours;
                coordLock.lock();
                try {
                    neighbours = edgeArrayIterator.next().clone();
                } finally {
                    coordLock.unlock();
                }

                if (neighbours.length > 0) {
                    for (int otherVertex : neighbours) {
                        if (otherVertex != 0) {
                            coordLock.lock();
                            int xOther = 0;
                            int yOther = 0;
                            try {
                                xOther = coordinates[otherVertex * 2 - 2];
                                yOther = coordinates[otherVertex * 2 - 1];
                            } finally {
                                coordLock.unlock();
                            }

                            centralJumpX += xOther - x;
                            centralJumpY += yOther - y;

                            if ((Tstage == STAGE.EXPANSION || Tstage == STAGE.COOLDOWN) && neighbours.length > Tmin_edges && Math.sqrt(Math.pow(xOther - x, 2) + Math.pow(yOther - y, 2)) > Tcut_off_length) {
                                otherVertex = 0;
                            }
                        }
                    }
                    centralJumpX = centralJumpX / neighbours.length;
                    centralJumpY = centralJumpY / neighbours.length;
                    double damping = 1.0 - Tdamping_mult;
                    centralJumpX = (int) (damping * coordinates[currentVertex * 2 - 2] + (1.0 - damping) * centralJumpX);
                    centralJumpY = (int) (damping * coordinates[currentVertex * 2 - 1] + (1.0 - damping) * centralJumpY);
                    if (centralJumpX < 0) {centralJumpX = 0;}
                    if (centralJumpX > Twidth) {centralJumpX = Twidth;}
                    if (centralJumpY < 0) {centralJumpY = 0;}
                    if (centralJumpY > Theight) {centralJumpY = Theight;}
                }



                randomJumpX = (int) (x + ((0.5 - rand.nextDouble()) * temperature));
                randomJumpY = (int) (y + ((0.5 - rand.nextDouble()) * temperature));

                if (randomJumpX < 0) {randomJumpX = 0;}
                if (randomJumpX > Twidth) {randomJumpX = Twidth;}
                if (randomJumpY < 0) {randomJumpY = 0;}
                if (randomJumpY > Theight) {randomJumpY = Theight;}

                coordLock.lock();
                try {
                    centralJumpEnergy = GetEnergy(neighbours, centralJumpX, centralJumpY);
                    randomJumpEnergy = GetEnergy(neighbours, randomJumpX, randomJumpY);
                    NoJumpEnergy = GetEnergy(neighbours, x, y);


                    if (randomJumpEnergy < centralJumpEnergy && randomJumpEnergy < NoJumpEnergy) {
                        coordinates[currentVertex * 2 - 2] = randomJumpX;
                        coordinates[currentVertex * 2 - 1] = randomJumpY;
                        density[randomJumpX / 50][randomJumpY / 50]++;
                    } else if (centralJumpEnergy < NoJumpEnergy) {
                        coordinates[currentVertex * 2 - 2] = centralJumpX;
                        coordinates[currentVertex * 2 - 1] = centralJumpY;
                        density[centralJumpX / 50][centralJumpY / 50]++;
                    } else {
                        density[x / 50][y / 50]++;
                    }
                    density[x / 50][y / 50]--;
                } finally {
                    coordLock.unlock();
                }

            }
        }
    }
    
    private void RunThreads() {
        List<Thread> list = new ArrayList<Thread>();
        for (int i = 0; i < threadCount - 1; i++){
            list.add(new VertexMovementLoopThread((vertexCount/threadCount * i) + 1, (vertexCount/threadCount * (i+1))));
            //System.out.println(((vertexCount/threadCount * i) + 1) + " " + (vertexCount/threadCount * (i+1)));
        }
        list.add(new VertexMovementLoopThread((vertexCount/threadCount) * (threadCount-1) + 1, vertexCount)); //make sure no vertex got rounded out
        //System.out.println(((vertexCount/threadCount) * (threadCount-1) + 1) + " " +  vertexCount);

        for (Thread t : list) {t.start();}
        try { for (Thread t : list) {t.join();}}
        catch (InterruptedException e) {throw new RuntimeException(e);}
    }

    private void VertexMovementLoop() {
        Iterator<int[]> edgeArrayIterator = edges.iterator();
        for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {

            int x = coordinates[currentVertex * 2 - 2];
            int y = coordinates[currentVertex * 2 - 1];

            int centralJumpX = 0;
            int centralJumpY = 0;
            double centralJumpEnergy = 0;
            int randomJumpX = 0;
            int randomJumpY = 0;
            double randomJumpEnergy = 0;
            double NoJumpEnergy = 0;

            int[] neighbours = edgeArrayIterator.next();

            if (neighbours.length != 0) {


                for (int otherVertex : neighbours) {
                    if (otherVertex != 0) {
                        int xOther = coordinates[otherVertex * 2 - 2];
                        int yOther = coordinates[otherVertex * 2 - 1];

                        centralJumpX += xOther - x;
                        centralJumpY += yOther - y;

                        if ((stage == STAGE.EXPANSION || stage == STAGE.COOLDOWN) && neighbours.length > min_edges && Math.sqrt(Math.pow(xOther - x, 2) + Math.pow(yOther - y, 2)) > cut_off_length) {
                            otherVertex = 0;
                        }
                    }
                }
                centralJumpX = centralJumpX / neighbours.length;
                centralJumpY = centralJumpY / neighbours.length;
            }

            double damping = 1.0 - damping_mult;
            centralJumpX = (int) (damping * coordinates[currentVertex * 2 - 2] + (1.0 - damping) * centralJumpX);
            centralJumpY = (int) (damping * coordinates[currentVertex * 2 - 1] + (1.0 - damping) * centralJumpY);

            if (centralJumpX < 0) {centralJumpX = 0;}
            if (centralJumpX > width) {centralJumpX = width;}
            if (centralJumpY < 0) {centralJumpY = 0;}
            if (centralJumpY > height) {centralJumpY = height;}

            centralJumpEnergy = GetEnergy(neighbours, centralJumpX, centralJumpY);

            randomJumpX = (int) (x + ((0.5 - rand.nextDouble()) * temperature));
            randomJumpY = (int) (y + ((0.5 - rand.nextDouble()) * temperature));

            if (randomJumpX < 0) {randomJumpX = 0;}
            if (randomJumpX > width) {randomJumpX = width;}
            if (randomJumpY < 0) {randomJumpY = 0;}
            if (randomJumpY > height) {randomJumpY = height;}

            randomJumpEnergy = GetEnergy(neighbours, randomJumpX, randomJumpY);

            NoJumpEnergy = GetEnergy(neighbours, x, y);

            if (randomJumpEnergy < centralJumpEnergy && randomJumpEnergy < NoJumpEnergy) {
                coordinates[currentVertex * 2 - 2] = randomJumpX;
                coordinates[currentVertex * 2 - 1] = randomJumpY;
                density[randomJumpX / 50][randomJumpY / 50]++;
                density[x/50][y/50]--;
            } else if (centralJumpEnergy < NoJumpEnergy) {
                coordinates[currentVertex * 2 - 2] = centralJumpX;
                coordinates[currentVertex * 2 - 1] = centralJumpY;
                density[centralJumpX / 50][centralJumpY / 50]++;
                density[x / 50][y / 50]--;
            }
            //UpdateDensity();
        }
    }

    private void ChangeParameters(){
        switch (stage) {
            case LIQUID:
                break;
            case EXPANSION:
                if (attraction > 1) {attraction -= 0.05; }
                if (cut_off_length > min_cut_off_length) { cut_off_length -= cut_rate; }
                if (damping_mult > 0.1) {damping_mult -= 0.005; };
                if (min_edges > 12) { min_edges -= 0.05; };
                break;
            case COOLDOWN:
                if (temperature > 500) { temperature -= 10; }
                if (cut_off_length > min_cut_off_length) {cut_off_length -= cut_rate*2; }
                if (min_edges > 12) { min_edges -= 0.05; };
                break;
            case CRUNCH:
                if (temperature > 50) {temperature -= 2; }
                break;
            case SIMMER:
                break;
            case DONE:
                break;
            default:
        }
    }

    private void InitParameters(){
        switch (stage) {
            case LIQUID:
                temperature = 2000;
                attraction = 2;
                damping_mult = 1.0;
                min_edges = 20;
                break;
            case EXPANSION:
                temperature = 2000;
                attraction = 10;
                damping_mult = 1.0;
                cut_off_length = longestEdge;
                min_cut_off_length = longestEdge * (1 - edgeCuttingParameter);
                cut_rate = (longestEdge - min_cut_off_length) / 400;
                min_edges = 20;
                break;
            case COOLDOWN:
                temperature = 500;
                attraction = 1;
                damping_mult = .25;
                cut_off_length = longestEdge;
                min_cut_off_length = longestEdge * (1 - edgeCuttingParameter);
                cut_rate = (longestEdge - min_cut_off_length) / 400;
                min_edges = 12;
                break;
            case CRUNCH:
                temperature = 500;
                attraction = 1;
                damping_mult = .25;
                cut_off_length = longestEdge * (1 - edgeCuttingParameter);
                break;
            case SIMMER:
                temperature = 500;
                attraction = 0.5;
                damping_mult = 0.0;
                break;
            case DONE:
                break;
            default:
        }
    }
    private double GetEnergy(int[] neighbours, int x, int y) {
        double res = 0;
        double attraction_factor = Math.pow(attraction,4)*2e-2;
        for (int i = 0; i < neighbours.length; i++) {
            int n = neighbours[i];
            res += (Math.pow(coordinates[2*n - 2] - x, 2) + Math.pow(coordinates[2*n - 1] - y, 2)) * attraction_factor;
        }
        if (stage != STAGE.SIMMER){
            res += (density[x/50][y/50] + 1) * 100;
            return res;
        }
        else {
            int d = 0;
            for (int i = 0; i < vertexCount; i++) {
                if (Math.pow(coordinates[2 * i] - x, 2) + Math.pow(coordinates[2 * i + 1] - y, 2) < 2500) {
                    d++;
                }
            }
            res += d * 10;
        }
        return res + 10;
    }

    private void UpdateDensity() {
        density = new int[width/50 + 1][height/50 + 1];
        for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {
            int x = coordinates[currentVertex * 2 - 2];
            int y = coordinates[currentVertex * 2 - 1];
            density[x/50][y/50]++;
        }
    }

    private void RemoveCutEdges() {
        Iterator<int[]> edgeArrayIterator = edges.iterator();
        for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {
            int[] neighbours = edgeArrayIterator.next();

            int nonCutCount = 0;
            for (int i = 0; i < neighbours.length; i++) {
                if (neighbours[i] != 0) {
                    nonCutCount++;
                }
            }

            int[] keptEdges = new int[nonCutCount];
            int index = 0;
            for (int i = 0; i < neighbours.length; i++) {
                if (neighbours[i] != 0) {
                    keptEdges[index] = neighbours[i];
                    index++;
                }
            }
            neighbours = keptEdges;
        }
    }

    private void UpdateLongestEdge() {

        //Update longest edge
        longestEdge = 0;
        Iterator<int[]> edgeArrayIterator = edges.iterator();
        for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {

            int x = coordinates[currentVertex * 2 - 2];
            int y = coordinates[currentVertex * 2 - 1];

            int[] neighbours = edgeArrayIterator.next();

            for (int otherVertex : neighbours) {
                int xOther = coordinates[otherVertex * 2 - 2];
                int yOther = coordinates[otherVertex * 2 - 1];

                double length = Math.sqrt(Math.pow(xOther - x, 2) + Math.pow(yOther - y, 2));
                if (length > longestEdge) {
                    longestEdge = length;
                }
            }
        }
    }

    public Graph<Integer, DefaultEdge> Coarsen() {
        // init datastuctures
        HashMap<Integer, List<Integer>> ClousterVertexes = new HashMap<Integer, List<Integer>>();

        for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {
            CoarsenedMap[currentVertex-1] = currentVertex;
            List<Integer> list = new ArrayList<Integer>();
            list.add(currentVertex);
            ClousterVertexes.put(currentVertex, list);

            Clousters[currentVertex-1][0] = 1;
            Clousters[currentVertex-1][1] = coordinates[currentVertex * 2 - 2];
            Clousters[currentVertex-1][2] = coordinates[currentVertex * 2 - 1];
        }

        //System.out.println(Arrays.deepToString(Clousters));

        //merge clousters
        for (DefaultEdge e : graph.edgeSet()) {
            int v1 = graph.getEdgeSource(e);
            int v2 = graph.getEdgeTarget(e);

            int u1 = CoarsenedMap[v1 - 1];
            int u2 = CoarsenedMap[v2 - 1];

            if (u1 == u2) {continue;}
            //System.out.println(v1 + " " + v2 + " " + u1 + " " + u2);

            int c1 = Clousters[u1-1][0];
            int x1 = Clousters[u1-1][1];
            int y1 = Clousters[u1-1][2];

            int c2 = Clousters[u2-1][0];
            int x2 = Clousters[u2-1][1];
            int y2 = Clousters[u2-1][2];


            if ((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) < max_cluster_distance*max_cluster_distance) {
                List<Integer> u1v = ClousterVertexes.get(u1);
                List<Integer> u2v = ClousterVertexes.get(u2);

                for (Integer v : u2v) {
                    u1v.add(v);
                    CoarsenedMap[v-1] = u1;
                }
                ClousterVertexes.remove(u2);

                Clousters[u1-1][1] = ((x1 * c1) + (x2 * c2)) / (c1 + c2);
                Clousters[u1-1][2] = ((y1 * c1) + (y2 * c2)) / (c1 + c2);
                Clousters[u1-1][0] += c2;
                Clousters[u2-1][0] = 0;
            }
        }

        //create solution
        Graph<Integer, DefaultEdge> res = new SimpleGraph<>(DefaultEdge.class);

        Object[] TempClousterVertexesIDs = ClousterVertexes.keySet().toArray();
        ClousterVertexesIDs = new int[ClousterVertexes.keySet().size()];
        coarsenedCoordinates = new int[ClousterVertexes.keySet().size() * 2];

        for (int i = 0; i < ClousterVertexes.keySet().size(); i++) {
            res.addVertex(i+1);
            ClousterVertexesIDs[i] = (int) TempClousterVertexesIDs[i];
            coarsenedCoordinates[2*i] = Clousters[ClousterVertexesIDs[i]-1][1];
            coarsenedCoordinates[2*i + 1] = Clousters[ClousterVertexesIDs[i]-1][2];
        }

        for (DefaultEdge e : graph.edgeSet()) {
            int v1 = graph.getEdgeSource(e);
            int v2 = graph.getEdgeTarget(e);
            int u1 = CoarsenedMap[v1 - 1];
            int u2 = CoarsenedMap[v2 - 1];

            if (u1 == u2) {
                continue;
            }
            int newV1 = ArrayUtils.indexOf(ClousterVertexesIDs, u1) + 1;
            int newV2 = ArrayUtils.indexOf(ClousterVertexesIDs, u2) + 1;
            res.addEdge(newV1, newV2);
        }
        return res;
    }

    public void Refine() {
        for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {
            int clouster = CoarsenedMap[currentVertex - 1];
            clouster = ArrayUtils.indexOf(ClousterVertexesIDs, clouster) + 1;
            coordinates[2*currentVertex-2] = coarsenedCoordinates[2*clouster - 2];
            coordinates[2*currentVertex-1] = coarsenedCoordinates[2*clouster - 1];
        }
        Recenter();
    }

    public void Recenter() {
        int maxX = 0; int maxY = 0; int minX = 2147483647; int minY = 2147483647;
        for (int i = 0; i < vertexCount; i++) {
            if (coordinates[i*2] > maxX) { maxX = coordinates[i*2];}
            if (coordinates[i*2+1] > maxY) { maxY = coordinates[i*2+1];}
            if (coordinates[i*2] < minX) { minX = coordinates[i*2];}
            if (coordinates[i*2+1] < minY) { minY = coordinates[i*2+1];}
        }

        int w = maxX - minX;
        int h = maxY - minY;

        for (int i = 0; i < vertexCount; i++) {
            coordinates[i*2] -= minX;
            coordinates[i*2+1] -= minY;
            coordinates[i*2] += width/2;
            coordinates[i*2+1] += height/2;
            coordinates[i*2] -= w/2;
            coordinates[i*2+1] -= h/2;
        }
        UpdateDensity();
    }

    public void SetRecursion (int level, double distance) {
        multi_level_recursion_level = level;
        max_cluster_distance = distance;
    }

    public void SetCoordinates (int[] c) {
        coordinates = c;
    }

    @Override
    List<String> GetAguments() {
        List<String> res = new ArrayList<String>();
        res.add("integer,loopNum,Loop Number");
        res.add("double,edgeCutting, Edge-cutting parameter");
        res.add("double,recursionLevel, recursionLevel");
        res.add("integer,maxClusterDistance,Clouster Merging distance");
        return res;
    }

    @Override
    void SetAguments(List<String> args) {
        for( String arg : args ) {
            String[] params = arg.split(",");
            switch (params[0]) {
                case "edgeCutting": edgeCuttingParameter = Double.parseDouble(params[1]);  break;
                case "loopNum": loopNum = Integer.parseInt(params[1]);  break;
                case "recursionLevel": multi_level_recursion_level = (int) Double.parseDouble(params[1]);  break;
                case "maxClusterDistance": max_cluster_distance = Integer.parseInt(params[1]);  break;
            }
        }

    }

    protected void Draw(){
        int[] coordinates2 = coordinates.clone();

        /*int maxX = 0; int maxY = 0; int minX = 2147483647; int minY = 2147483647;
        for (int i = 0; i < vertexCount; i++) {
            if (coordinates2[i*2] > maxX) { maxX = coordinates2[i*2];}
            if (coordinates2[i*2+1] > maxY) { maxY = coordinates2[i*2+1];}
            if (coordinates2[i*2] < minX) { minX = coordinates2[i*2];}
            if (coordinates2[i*2+1] < minY) { minY = coordinates2[i*2+1];}
        }

        maxX += 10; maxY += 10;
        int w = maxX - minX;
        int h = maxY - minY;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);*/

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setColor(Color.BLUE);
        for (int i = 0; i < width/50 + 1; i++) {
            for (int j = 0; j < height/50 + 1; j++) {
                g2d.drawRect(i * 50,j * 50,50,50);
                //System.out.print(density[i][j]);
            }
            //System.out.println();
        }


        /*for (int i = 0; i < vertexCount; i++) {
            coordinates2[i*2] += (5 - minX);
            coordinates2[i*2+1] += (5 - minY);
        }*/


        //System.out.println(vertexCount);

        BasicStroke edgeStroke = new BasicStroke(2);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(edgeStroke);
        Set<DefaultEdge> edges = graph.edgeSet();
        for (DefaultEdge edge : edges) {
            int sourceVertex = graph.getEdgeSource(edge);
            int targetVertex = graph.getEdgeTarget(edge);
            g2d.drawLine(coordinates2[2*sourceVertex - 2], coordinates2[2*sourceVertex - 1],coordinates2[2*targetVertex - 2], coordinates2[2*targetVertex - 1]);
        }

        g2d.setColor(Color.RED);

        for (int i = 0; i < vertexCount; i++) {
            g2d.fillOval(coordinates2[2*i] - 5, coordinates2[2*i + 1] - 5, 10, 10);
        }

        String filename = stage.toString() + ".png";
        try {
            File outputFile = new File(filename);
            ImageIO.write(img, "png", outputFile);
            //System.out.println("Image saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save Image");
        }
    }

    public static void main(String[] args) {
        OpenOrd Drawer = new OpenOrd();
        Drawer.Init(2000,1000, GraphLoader.Load());
        Drawer.PlaceVertexes();
    }
}
