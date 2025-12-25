package gui;

import paser.Networkpaser;

import wrapper.DataType.TrafficLightData;
import wrapper.DataType.VehicleData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.image.Image; 
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;

public class MapCanvas {
    private final Canvas canvas; // the drawing surface
    private final GraphicsContext g; // for drawing
    private Networkpaser.NetworkModel model; // the network model to render
    private final Transform transform; // coordinate transformation manager
    private View viewManager; // view manager for zooming/panning

    private List<VehicleData> vehicleDataList = new ArrayList<>();
    private List<TrafficLightData> trafficLightDataList = new ArrayList<>();

    protected Image vehicleTexture = new Image("file:../texture/humveeV2.png");
    protected double lastDragX = 0, lastDragY = 0; // last mouse drag positions
    //...
    // states[i] applies to the controlled link from[i] -> to[i]

    // Constructor of MapCanvas
    public MapCanvas(double w, double h) {
        canvas = new Canvas(w, h);
        g = canvas.getGraphicsContext2D();
        transform = new Transform(h);
        viewManager = new View(canvas, transform, null);

        // Mouse event handlers for panning and zooming
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastDragX = e.getX();
            lastDragY = e.getY();
            viewManager.startPan(e.getX(), e.getY());
        });
        // render on drag events
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            viewManager.updatePan(e.getX(), e.getY());
            lastDragX = e.getX();
            lastDragY = e.getY();
            render();
        });

        // render on scroll events
        canvas.addEventHandler(ScrollEvent.SCROLL, e -> {
            double factor = (e.getDeltaY() > 0) ? 1.1 : 0.9;
            viewManager.zoompoint(factor, e.getX(), e.getY());
            render();
        });
    }

    public Canvas getCanvas() { return canvas; }

    // Set the network model to be rendered
    public void setModel(Networkpaser.NetworkModel model) {
        this.model = model;
        this.viewManager = new View(canvas, transform, model);
        this.viewManager.resetView();
    }

    // Set vehicle data for rendering
    public void setVehicleData(List<VehicleData> vehicles) {
        this.vehicleDataList = (vehicles != null) ? vehicles : List.of();
    }


//...
    // Set traffic light data for rendering (call from wrapper)
    public void setTrafficLightData(List<TrafficLightData> trafficLights) {
        this.trafficLightDataList = (trafficLights != null) ? trafficLights : List.of();
    }


    // Offset polyline points by distance d
    private List<Point2D> offsetPolyline(List<Point2D> pts, double d) {
        if (pts.size() < 2) return pts;
        List<Point2D> out = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            Point2D p = pts.get(i);
            Point2D dir;
            if (i == 0) {
                dir = pts.get(i + 1).subtract(p);
            } else if (i == pts.size() - 1) {
                dir = p.subtract(pts.get(i - 1));
            } else {
                Point2D d1 = p.subtract(pts.get(i - 1));
                Point2D d2 = pts.get(i + 1).subtract(p);
                dir = d1.add(d2);
            }
            double len = Math.hypot(dir.getX(), dir.getY());
            if (len == 0) len = 1;
            double nx = -dir.getY() / len;
            double ny =  dir.getX() / len;
            out.add(new Point2D(p.getX() + nx * d, p.getY() + ny * d));
        }
        return out;
    }
    // Draw polyline from list of points
    private void drawPolyline(GraphicsContext g, List<Point2D> pts) {
        for (int i = 1; i < pts.size(); i++) {
            Point2D a = pts.get(i - 1);
            Point2D b = pts.get(i);
            g.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
    }

    public void render() {
        if (model == null) return;
        // Clear canvas
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw junctions
        Color roadFill = Color.web("#848484ff");
        g.setFill(roadFill);
        for (Networkpaser.Junction j : model.junctions) {
            if (j.shapePoints == null || j.shapePoints.size() < 3) continue;
            double[] xs = new double[j.shapePoints.size()];
            double[] ys = new double[j.shapePoints.size()];
            for (int i = 0; i < j.shapePoints.size(); i++) {
                Point2D p = j.shapePoints.get(i);
                xs[i] = transform.worldscreenX(p.getX());
                ys[i] = transform.worldscreenY(p.getY());
            }
            g.fillPolygon(xs, ys, xs.length);
        }

        // add sizes for roads
        double roadsize = 1.65;     
        double centermarksize = 0.5;  

        // Convert to pixels based on current transform (scale * zoom)
        double roadsizePx = transform.worldscreenSize(roadsize);
        double centermarksizePx = transform.worldscreenSize(centermarksize);
        // Draw roads
        for (Networkpaser.Edge e : model.edges) { // skip internal edges
            if (e.id.startsWith(":")) continue;
            for (Networkpaser.Lane lane : e.lanes) {
                if (lane.shapePoints.size() < 2) continue;
                List<Point2D> screenPts = new ArrayList<>(); // transformed points
                for (Point2D p : lane.shapePoints) {
                    screenPts.add(new Point2D(
                        transform.worldscreenX(p.getX()),
                        transform.worldscreenY(p.getY())
                    ));
                }
                g.setStroke(roadFill); 
                g.setLineWidth(roadsizePx * 2); // full road width in px
                g.setLineDashes();
                drawPolyline(g, screenPts);

                // Draw center line inside the road
                List<Point2D> centerline = offsetPolyline(screenPts, 0.0);
                g.setStroke(Color.web("#ffffffff"));
                g.setLineWidth(centermarksizePx);
                g.setLineDashes(18, 12); // optionally scale dash lengths too
                drawPolyline(g, centerline);
            }
        }

        // draw vehicles
        final double VEHICLE_LENGTH = 4;
        final double VEHICLE_WIDTH = 2;
        final double VEHICLE_LENGTH_PX = transform.worldscreenSize(VEHICLE_LENGTH);
        final double VEHICLE_WIDTH_PX = transform.worldscreenSize(VEHICLE_WIDTH);

        for (VehicleData vehData : vehicleDataList) {
            if (Double.isNaN(vehData.getPositionX(0))) {continue;}
            
            double screenX = transform.worldscreenX(vehData.getPositionX(0));
            double screenY = transform.worldscreenY(vehData.getPositionY(0));
            double screenAngle = 90 - vehData.getAngle(0);
            
            g.save();
            g.translate(screenX, screenY);
            g.rotate(screenAngle);
            // Image vehicleTexture = new Image("../texture/humveeV2.png");
            // g.setFill(pattern);
            g.setFill(vehData.getColor(0));//need fix
            // draw centered rectangle
            g.fillRect(-VEHICLE_LENGTH_PX, -VEHICLE_WIDTH_PX / 2.0, VEHICLE_LENGTH_PX, VEHICLE_WIDTH_PX);
            g.drawImage(vehicleTexture, -VEHICLE_LENGTH_PX, -VEHICLE_WIDTH_PX / 2.0, VEHICLE_LENGTH_PX, VEHICLE_WIDTH_PX);
            g.restore();
        }

        // draw traffic lights as lane-end bars similar to SUMO GUI
        final double BAR_LENGTH = transform.worldscreenSize(2.0);
        final double BAR_WIDTH = transform.worldscreenSize(0.6);
        for (TrafficLightData tlData : trafficLightDataList) {
            int n = tlData.getControlledLinksNum();
            for (int i = 0; i < n; i++) {
                List<String> defFromTo = tlData.getDefFromTo(i);
                Networkpaser.Lane lane = findLaneById(defFromTo.get(1));
                if (lane == null || lane.shapePoints.size() < 2) continue;
                // use the last segment of the lane polyline to place the bar
                Point2D p2 = lane.shapePoints.get(lane.shapePoints.size()-1);
                Point2D p1 = lane.shapePoints.get(lane.shapePoints.size()-2);
                // transform to screen
                Point2D s1 = new Point2D(transform.worldscreenX(p1.getX()), transform.worldscreenY(p1.getY()));
                Point2D s2 = new Point2D(transform.worldscreenX(p2.getX()), transform.worldscreenY(p2.getY()));
                // direction and normal
                Point2D dir = s2.subtract(s1);
                double len = Math.hypot(dir.getX(), dir.getY());
                if (len == 0) continue;
                Point2D unit = new Point2D(dir.getX()/len, dir.getY()/len);
                Point2D normal = new Point2D(-unit.getY(), unit.getX());
                // center of bar slightly before junction along lane direction
                double cx = s2.getX() - unit.getX() * transform.worldscreenSize(1.0);
                double cy = s2.getY() - unit.getY() * transform.worldscreenSize(1.0);
                // bar endpoints across lane using normal
                double hx = normal.getX() * (BAR_LENGTH/2.0);
                double hy = normal.getY() * (BAR_LENGTH/2.0);
                double x1 = cx - hx, y1 = cy - hy;
                double x2 = cx + hx, y2 = cy + hy;
                // color by state
                Color c;
                char st = defFromTo.get(0).charAt(0);
                //System.out.println("=="+st+"==");
                if (st == 'r') {c = Color.RED;}
                else if (st == 'y') {c = Color.YELLOW;}
                else if (st == 'g' || st == 'G') {c = Color.LIMEGREEN;}
                else c = Color.GRAY;
                g.setStroke(c);
                g.setLineWidth(BAR_WIDTH);
                g.setLineDashes();
                g.strokeLine(x1, y1, x2, y2);
            }
        }
    }

    private Networkpaser.Lane findLaneById(String laneId) {
        if (laneId == null || model == null) return null;
        for (Networkpaser.Edge e : model.edges) {
            for (Networkpaser.Lane l : e.lanes) {
                if (laneId.equals(l.id)) return l;
            }
        }
        return null;
    }

    public void fitAndCenter() {
        if (model == null) return;
        viewManager.resetView();
    }

    public void zoomAtCenter(double factor) {
        viewManager.zoomcenter(factor);
        render();
    }
}