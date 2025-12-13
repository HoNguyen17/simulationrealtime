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

public class MapCanvas {
    private final Canvas canvas; // the drawing surface
    private final GraphicsContext g; // for drawing
    private final Map<String, VehicleSprite> vehicleSprites = new HashMap<>();
    private Networkpaser.NetworkModel model; // the network model to render
    private final Transform transform; // coordinate transformation manager
    private View viewManager; // view manager for zooming/panning
    private List<VehicleData> vehicleDataList = new ArrayList<>();

    protected  double lastDragX = 0, lastDragY = 0; // last mouse drag positions

    public static record VehicleData(String id, double x, double y, double angle, Color color) {}
    



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



    private static class VehicleSprite {
        final String id;
        double worldX, worldY;
        double angle; // in degrees
        Color color;

        VehicleSprite(String id, double x, double y, double sumoAngleDeg, Color color) {
            this.id = id;
            updatePosition(new double[]{x, y, sumoAngleDeg});
            this.color = color;
        }

        public void updatePosition(double[] sumoData) {
            this.worldX = sumoData[0];
            this.worldY = sumoData[1];
            if (sumoData.length > 2) {
                // SUMO angle (deg) -> JavaFX screen angle (deg)
                double screenAngle = - (90.0 - sumoData[2]);
                this.angle = screenAngle;
            }
            updateBounds();
        }

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

        // Draw roads
        Color roadFill = Color.web("#210303ff");
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
                g.setStroke(roadFill); 
                g.setLineWidth(roadsizePx * 2); // full road width in px
                g.setLineDashes();
                drawPolyline(g, screenPts);

                // Draw center line inside the road
                List<Point2D> centerline = offsetPolyline(screenPts, 0.0);
                g.setStroke(Color.web("#bb87a7ff"));
                g.setLineWidth(centermarksizePx);
                g.setLineDashes(18, 12); // optionally scale dash lengths too
                drawPolyline(g, centerline);
            }
        }

        // Draw junctions
        Color junctionFill = Color.web("#210303ff");
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
            g.fillPolygon(xs, ys, xs.length);
        }




        // draw vehicles
        final double VEHICLE_LENGTH = 4.5;
        final double VEHICLE_WIDTH = 1.5;
        final double VEHICLE_LENGTH_PX = transform.worldscreenSize(VEHICLE_LENGTH);
        final double VEHICLE_WIDTH_PX = transform.worldscreenSize(VEHICLE_WIDTH);

        for (VehicleSprite sprite : vehicleSprites.values()) {
            double screenX = transform.worldscreenX(sprite.worldX);
            double screenY = transform.worldscreenY(sprite.worldY);

            g.save();
            g.translate(screenX, screenY);
            g.rotate(-sprite.angle);
            
            g.setFill(sprite.color);
            // draw centered rectangle
            g.fillRect(
                -VEHICLE_LENGTH_PX,
                -VEHICLE_WIDTH_PX / 2.0,
                VEHICLE_LENGTH_PX,
                VEHICLE_WIDTH_PX
            );
            g.restore();
        }
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
