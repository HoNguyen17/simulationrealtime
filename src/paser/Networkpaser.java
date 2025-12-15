package paser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javafx.geometry.Point2D;
import java.io.File;

/*
Networkpaser.java is responsible for parsing the network XML file
and constructing a NetworkModel object that encapsulates the network's
junctions, edges, and lanes.
*/


// Main class for parsing network XML
public class Networkpaser {
    // The class first defines four inner static classes to represent the core elements of a SUMO network model

    // class Junction
    public static class Junction { // represents an intersection or node in the road network
        public String id;
        // the geographic coordinates of the junction center
        public double x;
        public double y;
        public String type; // type of junction like "priority" or "traffic light"
        public String shape; // raw string of coordinates defining the junction's polygonal shape
        public List<Point2D> shapePoints = new ArrayList<>(); // A list storing the parsed coordinates of the shape
    }

        // Constructor of Junction class -> takes all field values as arguments and automatically calls parseShape to populate shapePoints
        public Junction(String id, double x, double y, String type, String shape) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.type = type;
            this.shape = shape;
            this.shapePoints = parseShape(shape);
        }

        // Constructor default
        public Junction() {}
    }


    // class lane
    public static class Lane { // represent a single lane within an edge
        public String id;
        public int index;
        public double speed;
        public double length;
        public double width;
        public List<Point2D> shapePoints = new ArrayList<>(); // the list of the point(take it in shape in lane(xml file)) <point2D> storing the parsed coordinates that define a lane's path

        // Constructor of Lane class -> takes all values and initializes the shapePoints list
        public Lane(String id, int index, double speed, double length, double width, List<Point2D> shapePoints) {
            this.id = id;
            this.index = index;
            this.speed = speed;
            this.length = length;
            this.width = width;
            if (shapePoints != null) this.shapePoints = shapePoints; else this.shapePoints = new ArrayList<>();
        }

        // Constructor default
        public Lane() {}
    }

    //class edge
    public static class Edge { // represent a road-segment
        public String id;
        public String from, to; // IDs of the junctions/nodes when connecting will make an edge
        public List<Lane> lanes; //<lane>

        // Constructor of Edge class
        public Edge(String id, String from, String to, List<Lane> lanes) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.lanes = lanes;
        }

        // Constructor default
        public Edge() {}
    }

    //class NetworkModel : use to overall all the Junction, Lane, Edge together
    public static class NetworkModel { // hold the entire parsed network data
        public List<Edge> edges; // all edges in the network can use in View.java,MapCanvas.java
        public List<Junction> junctions; // all junctions in the network can use in View.java,MapCanvas.java
        public double minX, maxX, minY, maxY; // bounds of the network are calculated during parsing to define the total geographic area covered by the network, which is useful for initial map centering and scaling, can use in View.java


        // Constructor of NetworkModel class
        public NetworkModel(List<Edge> edges, List<Junction> junctions, double minX, double maxX, double minY, double maxY) {
            this.edges = edges;
            this.junctions = junctions;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }

        // Constructor default
        public NetworkModel() {}
    }

    // Parse shape string into list of Point2D, e.g., "0,0 10,0 10,10" -> [(0,0), (10,0), (10,10)]
    private static List<Point2D> parseShape(String shape) { // convert coordinate strings found in the XML file into a usable list of Java Point2D objects
        List<Point2D> points = new ArrayList<>();

        // the input shape string is a series of space-separated coordinate pairs, like "x1,y1 x2,y2 x3,y3..."
        if (shape == null || shape.isEmpty()) return points;
        String[] coords = shape.trim().split(" "); // split the string by spaces to get individual "x,y" pairs
        for (String coord : coords) {
            String[] xy = coord.split(","); // splits each token by the comma to get the x and y values
            if (xy.length != 2) continue;
            try { // parse those tokens into double to create Point2D objects
                double x = Double.parseDouble(xy[0]);
                double y = Double.parseDouble(xy[1]);
                points.add(new Point2D(x, y));
            } catch (NumberFormatException e) {
                // Skip invalid coordinate
            }
        }
        return points;
    }

    // Main parsing method to create NetworkModel from XML file can use in App.java, View.java, MapCanvas.java
    // this is the central function that orchestrates the XML parsing using the Document Object Model (DOM) API
    public static NetworkModel parse(String path) throws Exception {
        File xmlFile = new File(path); //xml file path in App.java
        if (!xmlFile.exists()) { // verify whether the input XML file path is valid/exists
            throw new IOException("File not found: " + path);
        }

        /*
        use DocumentBuilder to parse the XML file and extract junctions, edges, and lanes.
        */
        // DOM Setup: initializes the DocumentBuilderFactory and DocumentBuilder to read the XML file into a DOM Document object
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile); // read file -> call dbuilder.parse(path) to load the SUMO network XML file into a Document object
        doc.getDocumentElement().normalize();

        // Parse junctions and Calculate bounds
        NodeList junctionNodes = doc.getElementsByTagName("junction"); // get all junction nodes/elements from xml file with the tag "junctions"
        List<Junction> junctions = new ArrayList<>(); // list of junctions
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE, minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE; // bounds initialization

        for (int i = 0; i < junctionNodes.getLength(); i++) { // iterate through all junction nodes, extract the attributes, and use the "Junction" constructor to create object
            Node node = junctionNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node; //catch element
                String id = elem.getAttribute("id");

                // the raw x and y coordinates are parsed directly
                double x = safeParseDouble(elem.getAttribute("x"), 0.0);
                double y = safeParseDouble(elem.getAttribute("y"), 0.0);
                String type = elem.getAttribute("type");
                String shape = elem.getAttribute("shape"); // shape will become x and y coordinates

                Junction junction = new Junction(id, x, y, type, shape);
                junctions.add(junction); // add junction to list

                // Update bounds by tracking the minimum and maximum "x" and "y" values to calculate the network bounds
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }

        // Parse edges and lanes
        NodeList edgeNodes = doc.getElementsByTagName("edge"); // retrieve all XML elements with the tag name "edge" from the XML file
        List<Edge> edges = new ArrayList<>(); // list of edges
        for (int i = 0; i < edgeNodes.getLength(); i++) { // iterate through all edge nodes and extract the attributes
            Node node = edgeNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                String id = elem.getAttribute("id");
                String from = elem.getAttribute("from");
                String to = elem.getAttribute("to");

                // Parse lanes within this edge
                NodeList laneNodes = elem.getElementsByTagName("lane"); // retrieve all child elements with the tag name "lane" from the XML file
                List<Lane> lanes = new ArrayList<>();
                for (int j = 0; j < laneNodes.getLength(); j++) { // iterate through all lane nodes and extract the attributes
                    Node laneNode = laneNodes.item(j);
                    if (laneNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element laneElem = (Element) laneNode;
                        String laneId = laneElem.getAttribute("id");
                        int index = safeParseInt(laneElem.getAttribute("index"), j);
                        double speed = safeParseDouble(laneElem.getAttribute("speed"), 0.0);
                        double length = safeParseDouble(laneElem.getAttribute("length"), 0.0);
                        double width = safeParseDouble(laneElem.getAttribute("width"), 3.0);
                        String shape = laneElem.getAttribute("shape");
                        List<Point2D> shapePoints = parseShape(shape);

                        lanes.add( new Lane(laneId, index, speed, length, width, shapePoints));
                    }
                }
                // Only add edges if it contains one or more "Lane" objects
                if(!lanes.isEmpty()) {
                    Edge edge = new Edge(id, from, to, lanes);
                    edges.add(edge);
                }
            }
        }
        return new NetworkModel(edges, junctions, minX, maxX, minY, maxY); // return the constructed NetworkModel
    }



    // These methods are designed to make the XML parsing process robust and prevent the application from crashing if the data in the SUMO network file (.net.xml) is missing, empty, or incorrectly formatted
    // Safe parsing helpers for double
    private static double safeParseDouble(String s, double def) {
        if (s == null || s.isEmpty()) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException ex) { return def; }
    }

    // Safe parsing helpers for int
    private static int safeParseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return def; }
    }
}