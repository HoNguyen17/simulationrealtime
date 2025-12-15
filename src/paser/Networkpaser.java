package paser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.geometry.Point2D;


// This class is responsible for reading and parsing a SUMO network XML file into an organized, and object-oriented data structure
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

    public static class Lane { // represent a single lane within an edge
        public String id;
        public double length;
        public List<Point2D> shapePoints = new ArrayList<>();// the list of the point(take it in shape in lane(xml file)) <point2D> storing the parsed coordinates that define a lane's path
    }

    // class edge
    public static class Edge { // represent a road-segment
        public String id;
        public String from, to; // IDs of the junctions/nodes when connecting will make an edge
        public List<Lane> lanes = new ArrayList<>(); //<lane>
    }

    //class Networkmodel : use to overrall all the Junction, Lane, Edge together
    public static class NetworkModel { // hold the entire parsed network data
        public List<Edge> edges = new ArrayList<>();
        public List<Junction> junctions = new ArrayList<>();
    }
    // method parse shape from string to list point2D
    private static List<Point2D> parseShape(String shape) { // convert coordinate strings found in the XML file into a usable list of Java Point2D objects
        List<Point2D> pts = new ArrayList<>();
        // the input shape string is a series of space-separated coordinate pairs, like "x1,y1 x2,y2 x3,y3..."
        if (shape == null || shape.isEmpty()) return pts;
        String[] tokens = shape.trim().split("\\s+"); // splits the string by spaces
        for (String t : tokens) {
            String[] xy = t.split(","); // splits each token by the comma to get the x and y values
            if (xy.length == 2) {
                try { // parse those tokens into double to create Point2D objects
                    double x = Double.parseDouble(xy[0]);
                    double y = Double.parseDouble(xy[1]);
                    pts.add(new Point2D(x, y));
                } catch (NumberFormatException ignore) {}
            }
        }
        return pts;
    }
    @SuppressWarnings("UseSpecificCatch")
    public static NetworkModel load(String path) throws Exception { // this is the central function that orchestrates the XML parsing using the Document Object Model (DOM) API
        // create factory and builder to read and parse file xml follow DOM lib
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(path); // read file -> call builder.parse(path) to load the SUMO network XML file into a Document object

        NetworkModel model = new NetworkModel(); // create an empty NetworkModel to hold the results

        // parse junctions
        // take a list all the element in junction
        NodeList jList = doc.getElementsByTagName("junction"); // retrieve all XML elements with the tag name "junction"
        for(int i=0; i < jList.getLength(); i++){ // iterate through the list, extract attributes like "id", "type", "x", "y", and "shape"
            Node n = jList.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE){
                //Element is in org.w3c.dom.Element and cast 
                Element j = (Element) n;
                Junction jj = new Junction();
                jj.id  = j.getAttribute("id");
                jj.type = j.getAttribute("type");
                jj.shape = j.getAttribute("shape"); //shape will become x and y coordinate
                // the raw x and y coordinates are parsed directly
                try { // use a catch to ??
                    jj.x = Double.parseDouble(j.getAttribute("x"));
                    jj.y = Double.parseDouble(j.getAttribute("y"));
                } catch (Exception ignore) {}
                jj.shapePoints = parseShape(jj.shape); // call parseShape method to parse shape string to list point2D
                model.junctions.add(jj); // each fully populated "junction" object is added to model.junctions
            }
        }

        // parse edges(+ lanes)
        NodeList edgeList = doc.getElementsByTagName("edge"); // retrieve all XML elements with the tag name "edge"
        for(int i = 0; i < edgeList.getLength(); i++){ // for each "edge" element, extract attributes like "id", "length", and "shape"
            Node e = edgeList.item(i);
            if(e.getNodeType() == Node.ELEMENT_NODE){
                Element edge = (Element) e;
                Edge ee = new Edge();
                ee.id = edge.getAttribute("id");
                ee.from = edge.getAttribute("from");
                ee.to = edge.getAttribute("to");

                // because lanes is a child in edge,you can see it in xml file
                NodeList laneList = edge.getElementsByTagName("lane"); // retrieve all child elements with the tag name "lane"
                for(int j = 0; j < laneList. getLength(); j++){ // for each "lane" element, extract attributes like "id", "length", and "shape"
                    Node l = laneList.item(j);
                    if(l.getNodeType() == Node.ELEMENT_NODE){
                        Element lane = (Element) l;
                        Lane ll = new Lane();
                        ll.id = lane.getAttribute("id");
                        try { // parse length from string to double
                            ll.length = Double.parseDouble(lane.getAttribute("length"));
                        } catch (Exception ignore) {
                        }
                        // parse x,y x,y ... from shape to coordinate x,y
                        String shape = lane.getAttribute("shape");
                        for(String pair : shape.split(" ")){
                            String[] xy = pair.split(",");
                            if(xy.length == 2){
                                try {
                                    double x = Double.parseDouble(xy[0]);
                                    double y = Double.parseDouble(xy[1]);
                                    ll.shapePoints.add(new Point2D(x, y)); // parse the "shape" attribute into Point2D objects
                                } catch (Exception ignore) {}
                            }

                        }
                        ee.lanes.add(ll); // add "lane" to the current edge's lanes list
                    }
                }
                model.edges.add(ee); // add the fully populated "edge" to model.edges
            }
        }

        return model; // return the model after parse all the element in xml file, containing all parsed junctions and edges
    }



    public static void main(String[] args) { // test case to demonstrate the DOM parsing logic for edges and lanes
        // the code help tap to xml file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            // return the document object and (=) give the name xml file you want parse
            // standard exception handling (try-catch blocks) is included to manage file and parsing errors
            Document doc =builder.parse("../resource/test_2_traffic.net.xml");
            NodeList edgeList = doc.getElementsByTagName("edge"); // handle the nodelist as edgelist

            // iterate through the "edge" tags and, for each edge, iterates through its nested "lane" tags
            for(int i=0; i<edgeList.getLength();i++){
                Node e = edgeList.item(i);
                if(e.getNodeType()==Node.ELEMENT_NODE){
                    // get the id of edge
                    Element edge  = (Element) e;
                    String id = edge.getAttribute("id");
                    // go inside a edge 
                    NodeList laneList = edge.getElementsByTagName("lane");
                    // look through laneList
                    for(int j=0; j < laneList.getLength(); j++){
                        Node l = laneList.item(j);

                        // next step: we need to grab the attribute of lane
                        if(l.getNodeType() == Node.ELEMENT_NODE){ // check if it is an element node
                            Element lane = (Element) l;
                            String laneId = lane.getAttribute("id");
                            String length = lane.getAttribute("length");
                            String shape = lane.getAttribute("shape");

                            System.out.println("Edge ID: " + id + ", lane ID: " + laneId + ", length: " + length + ", shape" + shape);
                        }
                    }

                }

            }





        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}