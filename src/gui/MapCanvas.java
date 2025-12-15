package gui;

import paser.Networkpaser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

/*
Encapsulate: 
Mapcanvas encapsulates the sprite management and rendering
logic for the network map, vehicles, and traffic lights.
It uses a Transform object for coordinate conversions.

Compostion:
- MapCanvas compose of:
  + Canvas: the drawing surface
  + GraphicsContext: for drawing operations
  + Transform: for coordinate transformation during rendering
  + View: for managing zoom/pan interactions
  + Sprites are modeled as inner classes (VehicleSprite, 
  TrafficLightSprite) that encapsulate per-entity state
  and update logic, scoped to MapCanvas.

*/
public class MapCanvas {
    private final Canvas canvas; // the drawing surface
    private final GraphicsContext g; // for drawing
    private final Map<String, VehicleSprite> vehicleSprites = new HashMap<>();
    private Networkpaser.NetworkModel model; // the network model to render
    private final Transform transform; // coordinate transformation manager
    private View viewManager; // view manager for zooming/panning
    private List<VehicleData> vehicleDataList = new ArrayList<>();
    private List<TrafficLightData> trafficLightDataList = new ArrayList<>();

    protected  double lastDragX = 0, lastDragY = 0; // last mouse drag positions

    public static record VehicleData(String id, double x, double y, double angle, Color color) {}
    
    // states[i] applies to the controlled link from[i] -> to[i]
    public static record TrafficLightData(String id, double x, double y, List<Character> states, List<String> fromLaneIds, List<String> toLaneIds) {}
    



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

    // Get the canvas
    public Canvas getCanvas() { return canvas; }

    // Set the network model to be rendered
    public void setModel(Networkpaser.NetworkModel model) {
        this.model = model;
        this.viewManager = new View(canvas, transform, model);
        this.viewManager.resetView();
    }


    // Inner class representing a vehicle sprite
    private static class VehicleSprite {
        final String id;
        double worldX, worldY;
        double angle; // in degrees
        Color color;

        // Constructor of VehicleSprite
        VehicleSprite(String id, double x, double y, double angleDeg, Color color) {
            this.id = id;
            updatePosition(new double[]{x, y, angleDeg});
            this.color = color;
        }


        // Update vehicle position and angle from SUMO data
        public void updatePosition(double[] sumoData) {
            this.worldX = sumoData[0];
            this.worldY = sumoData[1];
            if (sumoData.length > 2) {
                // Convert SUMO angle (0=East, CCW) to screen angle (0=North, CW)
                double screenAngle =  (90.0 - sumoData[2]);
                this.angle = screenAngle;
            }
            updateBounds();
        }

        // Update bounding box or other derived properties if needed
        private void updateBounds() {
        }
    }
    

    // Set vehicle data for rendering
    public void setVehicleData(List<VehicleData> vehicleDataList) {
        this.vehicleDataList = vehicleDataList;

        // update or create vehicle sprites
        for (VehicleData vd : vehicleDataList) {
            VehicleSprite sprite = vehicleSprites.get(vd.id());
            if (sprite == null) {
                sprite = new VehicleSprite(vd.id(), vd.x(), vd.y(), vd.angle(), vd.color());
                vehicleSprites.put(vd.id(), sprite);
            } else {
                sprite.color = vd.color();
                sprite.updatePosition(new double[]{vd.x(), vd.y(), vd.angle()});
            }
        }
        //delete sprites for vehicles no longer present
        vehicleSprites.keySet().removeIf(id ->
            vehicleDataList.stream().noneMatch(vd -> vd.id().equals(id))
        );
    }


    // Set traffic light data for rendering (call from wrapper)
    public void setTrafficLightData(List<TrafficLightData> trafficLights) {
        this.trafficLightDataList = trafficLights != null ? trafficLights : List.of(); // avoid null
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

        // Draw roads from network model parsed data
        Color roadFill = Color.web("#add9deff");
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
        double roadsize = 2.6;     
        double centermarksize = 0.5;  
        // Convert to pixels based on current transform (scale * zoom)
        double roadsizePx = transform.worldscreenSize(roadsize);
        double centermarksizePx = transform.worldscreenSize(centermarksize);
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
                g.setStroke(roadFill);  // road color
                g.setLineWidth(roadsizePx * 2); // full road width in px
                g.setLineDashes(); // solid line
                drawPolyline(g, screenPts);

                // Draw center line inside the road
                List<Point2D> centerline = offsetPolyline(screenPts, 0.0);
                g.setStroke(Color.web("#559f3bff"));
                g.setLineWidth(centermarksizePx);
                g.setLineDashes(18, 12); // optionally scale dash lengths too
                drawPolyline(g, centerline);
            }
        }

        // Draw junctions
        Color junctionFill = Color.web("#add9deff");
        g.setFill(junctionFill);
        for (Networkpaser.Junction j : model.junctions) {
            if (j.id != null && j.id.contains(":")) continue; // skip internal junctions
            if (j.shapePoints == null || j.shapePoints.size() < 3) continue;

            double[] xs = new double[j.shapePoints.size()];
            double[] ys = new double[j.shapePoints.size()];
            for (int i = 0; i < j.shapePoints.size(); i++) {
                Point2D p = j.shapePoints.get(i);
                xs[i] = transform.worldscreenX(p.getX());
                ys[i] = transform.worldscreenY(p.getY());
            }
            // fill a polygon shape for the junction
            g.fillPolygon(xs, ys, xs.length);
        }




        // draw vehicles
        final double VEHICLE_LENGTH = 4.5;
        final double VEHICLE_WIDTH = 1.5;
        final double VEHICLE_LENGTH_PX = transform.worldscreenSize(VEHICLE_LENGTH);
        final double VEHICLE_WIDTH_PX = transform.worldscreenSize(VEHICLE_WIDTH);

        for (VehicleSprite sprite : vehicleSprites.values()) { // draw each vehicle
            double screenX = transform.worldscreenX(sprite.worldX); // convert to screen coordinates
            double screenY = transform.worldscreenY(sprite.worldY);

            g.save(); // save current transform
            g.translate(screenX, screenY); // move to vehicle position
            g.rotate(sprite.angle); // rotate to vehicle heading
            
            g.setFill(sprite.color); // set vehicle color
            // draw centered rectangle
            g.fillRect(
                -VEHICLE_LENGTH_PX,
                -VEHICLE_WIDTH_PX / 2.0,
                VEHICLE_LENGTH_PX,
                VEHICLE_WIDTH_PX
            );
            g.restore(); // restore transform
        }


        // draw traffic lights as lane-end bars near junctions
        final double BAR_LENGTH = transform.worldscreenSize(2.0); // length of the bar
        final double BAR_WIDTH = transform.worldscreenSize(0.6); // width of the bar
        for (TrafficLightData tlData : trafficLightDataList) { // draw each traffic light
            List<Character> states = tlData.states(); // get states of the traffic light
            List<String> fromIds = tlData.fromLaneIds(); // get from-lane ids
            if (states == null || fromIds == null) continue;
            int n = Math.min(states.size(), fromIds.size()); // number of controlled links
            for (int i = 0; i < n; i++) {
                Networkpaser.Lane lane = findLaneById(fromIds.get(i)); // find lane by ID
                if (lane == null || lane.shapePoints.size() < 2) continue;
                // use the last segment of the lane polyline to place the bar
                Point2D p2 = lane.shapePoints.get(lane.shapePoints.size()-1);
                Point2D p1 = lane.shapePoints.get(lane.shapePoints.size()-2);
                // transform to screen
                Point2D s1 = new Point2D(transform.worldscreenX(p1.getX()), transform.worldscreenY(p1.getY()));
                Point2D s2 = new Point2D(transform.worldscreenX(p2.getX()), transform.worldscreenY(p2.getY()));
                // direction and normal
                Point2D dir = s2.subtract(s1); // lane direction vector
                double len = Math.hypot(dir.getX(), dir.getY()); // length of direction vector
                if (len == 0) continue;
                Point2D unit = new Point2D(dir.getX()/len, dir.getY()/len); // unit direction vector
                Point2D normal = new Point2D(-unit.getY(), unit.getX()); // normal vector to lane
                // center of bar slightly before junction along lane direction
                double centerx = s2.getX() - unit.getX() * transform.worldscreenSize(1.0);
                double centery = s2.getY() - unit.getY() * transform.worldscreenSize(1.0);
                // bar endpoints across lane using normal vector half-length
                double halfx = normal.getX() * (BAR_LENGTH/2.0);
                double halfy = normal.getY() * (BAR_LENGTH/2.0);

                // draw the bar
                double x1 = centerx - halfx, y1 = centery - halfy;
                double x2 = centerx + halfx, y2 = centery + halfy;
                // color by state
                Color c;
                char st = states.get(i);
                // set color based on state
                if (st == 'r' || st == 'R') c = Color.RED;
                else if (st == 'y' || st == 'Y') c = Color.YELLOW;
                else if (st == 'g' || st == 'G') c = Color.LIMEGREEN;
                else c = Color.GRAY;
                g.setStroke(c); // set stroke color
                g.setLineWidth(BAR_WIDTH); // set bar width
                g.setLineDashes(); // solid line
                g.strokeLine(x1, y1, x2, y2); // draw the bar
            }
        }
    }

    // Find lane by its ID in the network model
    private Networkpaser.Lane findLaneById(String laneId) {
        if (laneId == null || model == null) return null;
        for (Networkpaser.Edge e : model.edges) {
            for (Networkpaser.Lane l : e.lanes) {
                if (laneId.equals(l.id)) return l;
            }
        }
        return null;
    }


    // Fit and center the view to the network model
    public void fitAndCenter() {
        if (model == null) return;
        viewManager.resetView();
    }


    // Zoom at center of canvas
    public void zoomAtCenter(double factor) {
        viewManager.zoomcenter(factor);
        render();
    }
}