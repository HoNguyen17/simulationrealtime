package paser;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javafx.geometry.Point2D;
import java.io.File;

public class Networkpaser {

    public static class Junction {
        public String id;
        public double x;
        public double y;
        public String type;
        public String shape;
        public List<Point2D> shapePoints = new ArrayList<>();

        public Junction(String id, double x, double y, String type, String shape) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.type = type;
            this.shape = shape;
            this.shapePoints = parseShape(shape);
        }
        // Default constructor
        public Junction() {}
    }

    public static class Lane {
        public String id;
        public int index;
        public double speed;
        public double length;
        public double width;
        public List<Point2D> shapePoints = new ArrayList<>();

        public Lane(String id, int index, double speed, double length, double width, List<Point2D> shapePoints) {
            this.id = id;
            this.index = index;
            this.speed = speed;
            this.length = length;
            this.width = width;
            if (shapePoints != null) this.shapePoints = shapePoints; else this.shapePoints = new ArrayList<>();
        }
        // Default constructor
        public Lane() {}
    }

    public static class Edge {
        public String id;
        public String from;
        public String to;
        public List<Lane> lanes; 

        public Edge(String id, String from, String to, List<Lane> lanes) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.lanes = lanes;
        }
        // Default constructor
        public Edge() {}
    }

    public static class MidLine {
        public String id;
        public List<Point2D> line;

        public MidLine(String id, List<Point2D> line) {
            this.id = id;
            this.line = line;
        }
    }
    //class Networkmodel : use to overrall all the Junction, Lane, Edge together
    public static class NetworkModel {
        public List<Edge> edges;
        public List<Junction> junctions;
        public HashMap<String, MidLine> midLines;
        public double minX, maxX, minY, maxY;

        public NetworkModel(List<Edge> edges, List<Junction> junctions, HashMap<String, MidLine>midLines, double minX, double maxX, double minY, double maxY) {
            this.edges = edges;
            this.junctions = junctions;
            this.midLines = midLines;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
        // Default constructor
        public NetworkModel() {}
    }

    // Parse shape string into list of Point2D
    private static List<Point2D> parseShape(String shape) {
        List<Point2D> points = new ArrayList<>();
        if (shape == null || shape.isEmpty()) return points;
        String[] coords = shape.trim().split(" ");
        for (String coord : coords) {
            String[] xy = coord.split(",");
            if (xy.length != 2) continue;
            try {
                double x = Double.parseDouble(xy[0]);
                double y = Double.parseDouble(xy[1]);
                points.add(new Point2D(x, y));
            } catch (NumberFormatException e) {
                // Skip invalid coordinate
            }
        }
        return points;
    }
    

    public static NetworkModel parse(String path) throws Exception {
        File xmlFile = new File(path);
        if (!xmlFile.exists()) {
            throw new IOException("File not found: " + path);
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // Parse junctions
        NodeList junctionNodes = doc.getElementsByTagName("junction");
        List<Junction> junctions = new ArrayList<>();
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE, minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;

        for (int i = 0; i < junctionNodes.getLength(); i++) {
            Node node = junctionNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                String id = elem.getAttribute("id");
                double x = safeParseDouble(elem.getAttribute("x"), 0.0);
                double y = safeParseDouble(elem.getAttribute("y"), 0.0);
                String type = elem.getAttribute("type");
                String shape = elem.getAttribute("shape");

                Junction junction = new Junction(id, x, y, type, shape);
                junctions.add(junction);

                // Update bounds
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }

        // Parse edges
        NodeList edgeNodes = doc.getElementsByTagName("edge");
        List<Edge> edges = new ArrayList<>();
        HashMap<String, MidLine> midLines = new HashMap<>();
        for (int i = 0; i < edgeNodes.getLength(); i++) {
            Node node = edgeNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                String id = elem.getAttribute("id");
                String from = elem.getAttribute("from");
                String to = elem.getAttribute("to");

                // Parse lanes within this edge
                NodeList laneNodes = elem.getElementsByTagName("lane");
                List<Lane> lanes = new ArrayList<>();
                for (int j = 0; j < laneNodes.getLength(); j++) {
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
                        if (j == 0) {
                            if(!id.startsWith(":")) {
                                String tempID = id;
                                if(id.startsWith("-")) {tempID = id.substring(1);}
                                MidLine newline = new MidLine(tempID, shapePoints);
                                if (midLines.containsKey(tempID)) {
                                    MidLine oldline = midLines.get(tempID);
                                    newline = new MidLine(tempID, findMidLine(oldline, newline));
                                    midLines.put(tempID, newline);
                                }
                                else {midLines.put(tempID, newline);}
                            }
                        }
                    }
                }
                //Only add edges if it has lanes
                if(!lanes.isEmpty()) {
                    Edge edge = new Edge(id, from, to, lanes);
                    edges.add(edge);
                }
            }
        }
        return new NetworkModel(edges, junctions, midLines, minX, maxX, minY, maxY);
    }

    private static double safeParseDouble(String s, double def) {
        if (s == null || s.isEmpty()) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException ex) { return def; }
    }
    private static int safeParseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return def; }
    }
    private static List<Point2D> findMidLine(MidLine oldline, MidLine newline) {
        Point2D mid1 = (oldline.line.get(0)).midpoint(newline.line.get(1));
        Point2D mid2 = (oldline.line.get(1)).midpoint(newline.line.get(0));
        return List.of(mid1, mid2);
    }
}
