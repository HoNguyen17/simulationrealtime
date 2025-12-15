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

// This class manages the rendering of the static road network (from Networkpaser) and the dynamic entities (vehicles and traffic lights) by handling coordinate transformations, pan/zoom interactions, and object lifecycle (sprites)
public class MapCanvas {
    private final Canvas canvas; // the drawing surface
    private final GraphicsContext g; // for drawing
    private final Map<String, VehicleSprite> vehicleSprites = new HashMap<>(); // manages the dynamic objects to be drawn, uses a HashMap to quickly access and update VehicleSprite instances
    private Networkpaser.NetworkModel model; // the network model to render
    private final Transform transform; // coordinate transformation manager, convert from world coordinates <-> screen coordinates
    private View viewManager; // view manager for zooming/panning
    private List<VehicleData> vehicleDataList = new ArrayList<>();
    private List<TrafficLightData> trafficLightDataList = new ArrayList<>();

    protected  double lastDragX = 0, lastDragY = 0; // last mouse drag positions

    public static record VehicleData(String id, double x, double y, double angle, Color color) {} // real-time data for a vehicle

    // states[i] applies to the controlled link from[i] -> to[i]
    public static record TrafficLightData(String id, double x, double y, List<Character> states, List<String> fromLaneIds, List<String> toLaneIds) {} // real-time state and configuration for a traffic light




    // Constructor of MapCanvas
    public MapCanvas(double w, double h) {
        canvas = new Canvas(w, h);
        g = canvas.getGraphicsContext2D();
        transform = new Transform(h);
        viewManager = new View(canvas, transform, null);

        // Mouse event handlers for panning and zooming -> MOUSE_PRESSED records the start point, and MOUSE_DRAGGED continuously calls viewManager.updatePan(), causing the coordinate system (transform) to shift, and the map is re-rendered immediately
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

        // render on scroll events -> calculates a zoom factor (1.1 for in, 0.9 for out) and calls viewManager.zoompoint() to scale the view, centering the zoom operation on the cursor's location, and the map is re-rendered immediately.
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


    // Inner class representing a vehicle sprite to encapsulate the drawing state for each vehicle
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
                // performs a necessary coordinate system conversion: SUMO angle (0° = East, counter-clockwise increase) is converted to a Screen angle (0° = North, clockwise increase) using the formula (90.0 - sumoData[2])
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

        // update or create vehicle sprites -> iterates through the new VehicleData. If a VehicleSprite already exists for an ID, it's updated; otherwise, a new sprite is created and added to the map
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
        // delete sprites for vehicles no longer present
        vehicleSprites.keySet().removeIf(id ->
                vehicleDataList.stream().noneMatch(vd -> vd.id().equals(id))
        );
    }


    // Set traffic light data for rendering (call from wrapper)
    public void setTrafficLightData(List<TrafficLightData> trafficLights) {
        this.trafficLightDataList = trafficLights != null ? trafficLights : List.of(); // avoid null
    } // stores the list of traffic light states for use during rendering




    // Offset polyline points by distance d
    // Calculates a polyline offset perpendicular to the original path. (Used here to get the center line, where the offset distance d is 0)
    private List<Point2D> offsetPolyline(List<Point2D> pts, double d) { // take an input polyline (the center path of a lane) and calculate a new polyline that runs parallel to the original path, offset by a specified perpendicular distance d
        if (pts.size() < 2) return pts;
        List<Point2D> out = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            Point2D p = pts.get(i);
            Point2D dir; // Determine Direction Vector (dir): The code first calculates the vector representing the road's direction at point P
            if (i == 0) { // start point: dir is the vector from P0 to P1
                dir = pts.get(i + 1).subtract(p);
            } else if (i == pts.size() - 1) { // end point: dir is the vector from P(size-2) to P(size-1)
                dir = p.subtract(pts.get(i - 1));
            } else { // intermediate point (0 < i < size - 1): dir is the sum of the vector coming to Pi and the vector going from Pi
                Point2D d1 = p.subtract(pts.get(i - 1));
                Point2D d2 = pts.get(i + 1).subtract(p);
                dir = d1.add(d2);
            }
            double len = Math.hypot(dir.getX(), dir.getY());
            if (len == 0) len = 1;
            double nx = -dir.getY() / len;
            double ny =  dir.getX() / len;

            // calculate the offset point -> The new offset point Pout is calculated by moving the original point P along the normal vector n by the distance d
            out.add(new Point2D(p.getX() + nx * d, p.getY() + ny * d)); // Pout = P + n * d, this new point is added to the out list
        }
        return out;
    }
    // Draw polyline from list of points -> take a sequence of points that define a line path (a polyline) and render it onto the GraphicsContext
    private void drawPolyline(GraphicsContext g, List<Point2D> pts) {
        for (int i = 1; i < pts.size(); i++) { // iterates from the second point (index i=1) up to the end of the pts list
            // in each iteration, it draws a single straight line segment using g.strokeLine()
            Point2D a = pts.get(i - 1); // start point
            Point2D b = pts.get(i); // end point
            g.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
    } // the final result is a connected line path that follows all the points in the list

    public void render() {
        if (model == null) return;
        // Clear canvas -> fill the canvas with white
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw roads from network model parsed data
        Color roadFill = Color.web("#add9deff");
        g.setFill(roadFill);
        for (Networkpaser.Junction j : model.junctions) {
            if (j.shapePoints == null || j.shapePoints.size() < 3) continue;
            double[] xs = new double[j.shapePoints.size()];
            double[] ys = new double[j.shapePoints.size()];
            for (int i = 0; i < j.shapePoints.size(); i++) { // convert the polygonal shapePoints to screen coordinates using the "transform" object
                Point2D p = j.shapePoints.get(i);
                xs[i] = transform.worldscreenX(p.getX());
                ys[i] = transform.worldscreenY(p.getY());
            }
            g.fillPolygon(xs, ys, xs.length); // draws the intersection area
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
                // in short, it iterates through model.edges and lanes. It uses a calculated pixel width (roadsizePx) based on a world unit (roadsize) and calls the helper drawPolyline() to draw the road surface as a thick line

                // Draw center line inside the road -> uses offsetPolyline(screenPts, 0.0) to get the centerline path, then draws a dashed line over the road base
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
        // in short, it iterates through model.junctions, converts the polygonal shapePoints to screen coordinates using the transform object, and draws the intersection area using g.fillPolygon()




        // draw vehicles
        final double VEHICLE_LENGTH = 4.5;
        final double VEHICLE_WIDTH = 1.5;
        final double VEHICLE_LENGTH_PX = transform.worldscreenSize(VEHICLE_LENGTH);
        final double VEHICLE_WIDTH_PX = transform.worldscreenSize(VEHICLE_WIDTH);

        for (VehicleSprite sprite : vehicleSprites.values()) { // draw each vehicle
            double screenX = transform.worldscreenX(sprite.worldX); // convert to screen coordinates
            double screenY = transform.worldscreenY(sprite.worldY);

            g.save(); // save current graphics state
            g.translate(screenX, screenY); // move the origin to the vehicle's position
            g.rotate(sprite.angle); // rotate the coordinate system to the vehicle heading

            g.setFill(sprite.color); // set vehicle color
            // draw centered rectangle (represent the vehicle)
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
        for (TrafficLightData tlData : trafficLightDataList) { // iterate through trafficLightDataList, draw each traffic light
            List<Character> states = tlData.states(); // get states of the traffic light
            List<String> fromIds = tlData.fromLaneIds(); // get from-lane ids
            if (states == null || fromIds == null) continue;
            int n = Math.min(states.size(), fromIds.size()); // number of controlled links
            for (int i = 0; i < n; i++) {
                Networkpaser.Lane lane = findLaneById(fromIds.get(i)); // use findLaneById to get the geometry of the source lane
                if (lane == null || lane.shapePoints.size() < 2) continue;
                // use the last segment of the lane polyline to place the bar, to be precise, use the last two points of the lane's path to determine the direction and position near the junction
                Point2D p2 = lane.shapePoints.get(lane.shapePoints.size()-1);
                Point2D p1 = lane.shapePoints.get(lane.shapePoints.size()-2);
                // transform to screen
                Point2D s1 = new Point2D(transform.worldscreenX(p1.getX()), transform.worldscreenY(p1.getY()));
                Point2D s2 = new Point2D(transform.worldscreenX(p2.getX()), transform.worldscreenY(p2.getY()));
                // direction and normal -> calculate a position slightly before the end of the lane and a vector normal (perpendicular) to the lane's direction
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

                // draws a line (the "stop bar") perpendicular to the lane, using the color corresponding to the current state ('r' -> RED, 'g' -> LIMEGREEN, 'y' -> YELLOW).
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