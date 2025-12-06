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



public class Networkpaser {
    /*
    First we need to build 3 class for the network
    */

    //class Junction
    public static class Junction {
        public String id;
        public double x;
        public double y;
        public String type;
        public String shape;
        public List<Point2D> shapePoints = new ArrayList<>();
    }

    public static class Lane {
        public String id;
        public double length;
        public List<Point2D> shapePoints = new ArrayList<>();// the list of the point(take it in shape in lane(xml file)) <point2D>
    }

    //class edge
    public static class Edge {
        public String id;
        public String from, to;
        public List<Lane> lanes = new ArrayList<>(); //<lane>
    }

    //class Networkmodel : use to overrall all the Junction, Lane, Edge together
    public static class NetworkModel {
        public List<Edge> edges = new ArrayList<>();
        public List<Junction> junctions = new ArrayList<>();
    }
    //method parse shape from string to list point2D
     private static List<Point2D> parseShape(String shape) {
        List<Point2D> pts = new ArrayList<>();
        if (shape == null || shape.isEmpty()) return pts;
        String[] tokens = shape.trim().split("\\s+");
        for (String t : tokens) {
            String[] xy = t.split(",");
            if (xy.length == 2) {
                try {
                    double x = Double.parseDouble(xy[0]);
                    double y = Double.parseDouble(xy[1]);
                    pts.add(new Point2D(x, y));
                } catch (NumberFormatException ignore) {}
            }
        }
        return pts;
     }
    @SuppressWarnings("UseSpecificCatch")
    public static NetworkModel load(String path) throws Exception {
        //create factory to read and parse file xml follow DOM lib
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(path);

        NetworkModel model = new NetworkModel();
        
        //parse junctions
        //take a list all the element in junction
        NodeList jList = doc.getElementsByTagName("junction");
        for(int i=0; i < jList.getLength(); i++){
            Node n = jList.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE){
                //Element is in org.w3c.dom.Element and cast 
                Element j = (Element) n; 
                Junction jj = new Junction();
                jj.id  = j.getAttribute("id");
                jj.type = j.getAttribute("type");
                jj.shape = j.getAttribute("shape"); //shape will become x and y cordinate
                try { // use a catch to ??
                    jj.x = Double.parseDouble(j.getAttribute("x"));
                    jj.y = Double.parseDouble(j.getAttribute("y"));
                } catch (Exception ignore) {}
                jj.shapePoints = parseShape(jj.shape); // call parseShape method to parse shape string to list point2D
                model.junctions.add(jj); // a method you call in application to parse junctions
            }
        }

        //parse edges(+ lanes)
        NodeList edgeList = doc.getElementsByTagName("edge");
        for(int i = 0; i < edgeList.getLength(); i++){
            Node e = edgeList.item(i);
            if(e.getNodeType() == Node.ELEMENT_NODE){
                Element edge = (Element) e;
                Edge ee = new Edge();
                ee.id = edge.getAttribute("id");
                ee.from = edge.getAttribute("from");
                ee.to = edge.getAttribute("to");

                //because lanes is a child in edge,you can see it in xml file
                NodeList laneList = edge.getElementsByTagName("lane");
                for(int j = 0; j < laneList. getLength(); j++){
                    Node l = laneList.item(j);
                    if(l.getNodeType() == Node.ELEMENT_NODE){
                        Element lane = (Element) l;
                        Lane ll = new Lane();
                        ll.id = lane.getAttribute("id");
                        try { // parse length from string to double
                            ll.length = Double.parseDouble(lane.getAttribute("length"));
                        } catch (Exception ignore) {
                        }
                        //parse x,y x,y ... from shape to cordinate x,y
                        String shape = lane.getAttribute("shape");
                        for(String pair : shape.split(" ")){
                            String[] xy = pair.split(",");
                            if(xy.length == 2){
                                try {
                                    double x = Double.parseDouble(xy[0]);
                                    double y = Double.parseDouble(xy[1]);
                                    ll.shapePoints.add(new Point2D(x, y));
                                } catch (Exception ignore) {}
                            }    

                        }
                        ee.lanes.add(ll); // add lane to edge
                    }
                }
                model.edges.add(ee); // add edge to model
            }
        }

        return model; // return the model after parse all the element in xml file
    }



    public static void main(String[] args) {
        // the code help tap to xml file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            // return the document object and (=) give the name xml file you want parse
            //need to catch try to have SAXException and IOException
            Document doc =builder.parse("../resource/test_2_traffic.net.xml");
            NodeList edgeList = doc.getElementsByTagName("edge"); // handle the nodelist as edgelist

            //look through your NodeList; it look through twice because we have 2 edge tag
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

                        //next step: we need to grab the attribute of lane
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
