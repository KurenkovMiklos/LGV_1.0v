import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class ForceAtlas2 extends VertexPlacementAlgorithm {
    List<String> currentArgs;
    private final double epsilon = 1e-16; // Math stability
    private double kr; //Repulsion Force
    private enum AttrMode {Classical, LinLog, Dissuade_Hubs};     private AttrMode attr = AttrMode.Classical;
    private enum GravMode {No, Weak, Strong};     private GravMode grav = GravMode.No;
    private double kg; //Gravitational Force
    private boolean nooverlap; //Prevent Overlapping
    private boolean swing; //Anti-Swinging
    private double tolarance = 1;
    int loopNum = 100;

    int vertexCount;
    int gravCenterX;
    int gravCenterY;


    int[] startCoordinates;
    int[] coordinates;
    int[] degrees;
    List<int[]>edges;

    HashMap<Integer, int[]> loopNumChache = new HashMap<Integer, int[]>(); //store coordinates for calculated positions

    public void Init(int width, int height, Graph<Integer, DefaultEdge> graph){
        vertexCount = graph.vertexSet().size();
        gravCenterX = width / 2;
        gravCenterY = height / 2;

        CircleAlgorithm ca = new CircleAlgorithm();
        ca.Init(width, height, graph);
        startCoordinates = ca.PlaceVertexes();

        degrees = new int[vertexCount];
        edges = new ArrayList<int[]>();

        for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {
            degrees[currentVertex-1] = graph.degreeOf(currentVertex);

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
    }

    public int[] PlaceVertexes() {
        //for (int[] currentEdges : edges) {
        //    System.out.println(edges.indexOf(currentEdges) + 1 + ": " + Arrays.toString(currentEdges));
        //}

        int StartloopNum = 0;
        for (Integer key : loopNumChache.keySet()) {
            if (key <= loopNum && key > StartloopNum){
                StartloopNum = key;
            }
        }
        if (StartloopNum == 0) {
            coordinates = startCoordinates.clone();
        } else {
            coordinates = loopNumChache.get(StartloopNum).clone();
        }


        //System.out.println(StartloopNum);


        for (int loopCounter = StartloopNum; loopCounter < loopNum; loopCounter++) {

            double[] forces = new double[vertexCount * 2];
            double[] forcesPrev = new double[vertexCount * 2];

            double globalSpeedPrev = 1;

            Iterator<int[]> edgeArrayIterator = edges.iterator();

            for (int currentVertex = 1; currentVertex <= vertexCount; currentVertex++) {

                int x = coordinates[currentVertex * 2 - 2];
                int y = coordinates[currentVertex * 2 - 1];

                //Set<DefaultEdge> edges = graph.edgesOf(currentVertex);

                double AttractionX = 0;
                double AttractionY = 0;


                for (int otherVertex : edgeArrayIterator.next()) {

                    int xOther = coordinates[otherVertex * 2 - 2];
                    int yOther = coordinates[otherVertex * 2 - 1];

                    int xVector = xOther - x;
                    int yVector = yOther - y;
                    double distance = Math.max(epsilon, (Math.sqrt(xVector * xVector + yVector * yVector)));

                    double force = 0;
                    if (attr == AttrMode.Classical){
                        force = distance < 30 && nooverlap ? 0 : distance;
                    } else if (attr == AttrMode.LinLog) {
                        force = distance < 30 && nooverlap ? 0 : Math.log(distance + 1);
                    } else if (attr == AttrMode.Dissuade_Hubs) {
                        force = distance < 30 && nooverlap ? 0 : distance/(degrees[currentVertex-1]+1);
                    }

                    AttractionX += (xVector / distance) * force;
                    AttractionY += (yVector / distance) * force;

                    //System.out.println("force: " + Math.log(coordinates[otherVertex * 2 - 2] - x + 1) + " " + Math.log(coordinates[otherVertex * 2 - 1] - y + 1));
                }

                double RepulsionX = 0;
                double RepulsionY = 0;


                for (int i = 1; i <= vertexCount; i++) {
                    int xOther = coordinates[i * 2 - 2];
                    int yOther = coordinates[i * 2 - 1];

                    int xVector = xOther - x;
                    int yVector = yOther - y;
                    double distance = Math.max(epsilon, (Math.sqrt(xVector * xVector + yVector * yVector)));
                    double force = kr * (degrees[i - 1] + 1) * (degrees[currentVertex - 1] + 1) / distance;

                    RepulsionX -= (xVector / distance) * force;
                    RepulsionY -= (yVector / distance) * force;
                }

                double GravityX = 0;
                double GravityY = 0;

                if(grav != GravMode.No){
                        int xVector = gravCenterX - x;
                        int yVector = gravCenterY - y;
                        double distance = Math.max(epsilon, (Math.sqrt(xVector * xVector + yVector * yVector)));
                        double force = kg * (degrees[currentVertex - 1] + 1);
                        if (grav == GravMode.Strong) {force *= distance;}

                        GravityX += (xVector / distance) * force;
                        GravityY += (yVector / distance) * force;
                }



                double offsetX = AttractionX + RepulsionX + GravityX;
                double offsetY = AttractionY + RepulsionY + GravityY;
                double offset = Math.sqrt((offsetX * offsetX) + (offsetY * offsetY));
                if (offset > 50) {
                    offsetX /= offset; offsetY /= offset;
                    offsetX *= 50; offsetY *= 50;
                }

                forces[currentVertex * 2 - 2] = offsetX;
                forces[currentVertex * 2 - 1] = offsetY;
            }

            if (swing){

                double[] vetexSwinging = new double[vertexCount * 2];
                double[] vetexTraction = new double[vertexCount * 2];
                for (int i = 0; i < vertexCount; i++){
                    vetexSwinging[i] = forces[i] - forcesPrev[i];
                    vetexTraction[i] = (forces[i] + forcesPrev[i]) / 2;
                    //System.out.println(vetexSwinging[i]);
                    //System.out.println(vetexTraction[i]);
                }

                double[] vetexSwingingAbs = new double[vertexCount];
                double[] vetexTractionAbs = new double[vertexCount];
                double[] vetexForceAbs = new double[vertexCount];
                for (int i = 0; i < vertexCount; i++){
                    vetexSwingingAbs[i] = Math.sqrt(vetexSwinging[i*2] * vetexSwinging[i*2] + vetexSwinging[i*2+1] * vetexSwinging[i*2+1]);
                    vetexTractionAbs[i] = Math.sqrt(vetexTraction[i*2] * vetexTraction[i*2] + vetexTraction[i*2+1] * vetexTraction[i*2+1]);
                    vetexForceAbs[i] = Math.sqrt(forces[i*2] * forces[i*2] + forces[i*2+1] * forces[i*2+1]);
                    //System.out.println(vetexSwingingAbs[i]);
                    //System.out.println(vetexTractionAbs[i]);
                }

                double globalSwinging = 0;
                double globalTraction = 0;
                for (int i = 0; i < vertexCount; i++){
                    globalSwinging += vetexSwingingAbs[i] * (degrees[i] + 1);
                    globalTraction += vetexTractionAbs[i] * (degrees[i] + 1);
                }
                //System.out.println(globalSwinging);
                //System.out.println(globalTraction);

                double globalSpeed = Math.min(tolarance * globalTraction / globalSwinging, globalSpeedPrev * 1.5);
                globalSpeedPrev = globalSpeed;
                //System.out.println(globalSpeed);

                if (!Double.isNaN(globalSpeed)) {

                    double[] vetexSpeed = new double[vertexCount];
                    for (int i = 0; i < vertexCount; i++) {
                        vetexSpeed[i] = Math.min((0.1 * globalSpeed) / (1 + (globalSpeed * Math.sqrt(vetexSwingingAbs[i]))), 10 / vetexForceAbs[i]);
                        System.out.println(vetexSpeed[i]);
                    }

                    for (int i = 0; i < vertexCount; i++) {
                        coordinates[i * 2] += forces[i * 2] * vetexSpeed[i];
                        coordinates[i * 2 + 1] += forces[i * 2 + 1] * vetexSpeed[i];
                    }
                }

                //System.out.println("----------------------------------------------------------");
            } else {
                for (int i = 0; i < vertexCount * 2; i++) {
                    coordinates[i] += forces[i];
                }
            }

            forcesPrev = forces.clone();

        }

        loopNumChache.put(loopNum, coordinates.clone());
        return coordinates;
    }

    public List<String> GetAguments() {
        //start layout

        List<String> res = new ArrayList<String>();
        res.add("integer,loopNum,Loop Number");
        res.add("double,kr,Repulsion Force");
        res.add("enum,attr,Classical Attraction,LinLog Attraction,Dissuade Hubs");
        res.add("enum,grav,No Gravity,Weak Gravity,Strong Gravity");
        res.add("double,kg,Gravitational Force");
        res.add("boolean,nooverlap,Prevent Overlapping");
        res.add("boolean,swing,Anti-Swinging");
        res.add("double,tolarance,Tolarance");
        currentArgs = res;
        return res;
    }
    public void SetAguments(List<String> args) {


        //check weather the loop num is the only one changed. if so, chache may be used, otherwise clear chache
        List<String> args2 = new ArrayList<>(args);
        List<String> currentArgs2 = new ArrayList<>(currentArgs);
        String loopNumArg = "";
        for( String arg : args2 ) {
            String[] params = arg.split(",");
            if (params[0].equals("loopNum")){
                loopNumArg = arg;
            }
        }
        args2.remove(loopNumArg);
        for( String arg : currentArgs2 ) {
            String[] params = arg.split(",");
            if (params[0].equals("loopNum")){
                loopNumArg = arg;
            }
        }
        currentArgs2.remove(loopNumArg);
        if (!currentArgs2.equals(args2)) {loopNumChache.clear(); /*System.out.println("chache cleared");*/}



        for( String arg : args ) {
            String[] params = arg.split(",");
            switch (params[0]) {
                case "attr":
                    switch (params[1]) {
                        case "Classical Attraction": attr = AttrMode.Classical; break;
                        case "LinLog Attraction": attr = AttrMode.LinLog; break;
                        case "Dissuade Hubs": attr = AttrMode.Dissuade_Hubs; break;
                    }
                    break;
                case "grav":
                    switch (params[1]) {
                        case "No Gravity": grav = GravMode.No;  break;
                        case "Weak Gravity": grav = GravMode.Weak;  break;
                        case "Strong Gravity": grav = GravMode.Strong;  break;
                    }
                    break;
                case "kr": kr = Double.parseDouble(params[1]);  break;
                case "loopNum": loopNum = Integer.parseInt(params[1]);  break;
                case "kg": kg = Double.parseDouble(params[1]);  break;
                case "tolarance": tolarance = Double.parseDouble(params[1]);  break;
                case "nooverlap": nooverlap = params[1].equals("true");  break;
                case "swing": swing = params[1].equals("true");  break;
            }
        }
        currentArgs = args;

        //System.out.println(attr);
        //System.out.println(grav);
        //System.out.println(kr);
        //System.out.println(kg);
        //System.out.println(nooverlap);
        //System.out.println(swing);
    }

}