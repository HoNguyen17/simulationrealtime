package gui;

import paser.Networkpaser;
import wrapper.SimulationWrapper;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

public class MapCanvas {
    private final Canvas canvas;
    private final GraphicsContext g;

    private Networkpaser.NetworkModel model;
    private final Transform transform;
    private View viewManager;
    private List<VehicleData> vehicleDataList = new ArrayList<>();
    private SimulationWrapper simulationWrapper;

    protected  double lastDragX = 0, lastDragY = 0;

    public static record VehicleData(String id, double x, double y, double angle, Color color) {}

    // Constructor of MapCanvas
    public MapCanvas(double w, double h) {
        canvas = new Canvas(w, h);
        g = canvas.getGraphicsContext2D();
        transform = new Transform(h);
        viewManager = new View(canvas, transform, null);

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastDragX = e.getX();
            lastDragY = e.getY();
            viewManager.startPan(e.getX(), e.getY());
        });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            viewManager.updatePan(e.getX(), e.getY());
            lastDragX = e.getX();
            lastDragY = e.getY();
            render();
        });
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

    public void setVehicleData(List<VehicleData> vehicleDataList) {
        this.vehicleDataList = vehicleDataList;
    }

    public void setSimulationWrapper(SimulationWrapper simulationWrapper) {
        this.simulationWrapper = simulationWrapper;
    }

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
        double roadsize = 5.0;      // e.g. 4 meters half-width
        double centermarksize = 0.5;   // e.g. 15 cm

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
        g.setFill(Color.ORANGE);
        for (Networkpaser.Junction j : model.junctions) {
            double x = transform.worldscreenX(j.x);
            double y = transform.worldscreenY(j.y);
            // junction mark size in world units (e.g. 0.3m)
            double junctionDotWorld = 0.3;
            double r = transform.worldscreenSize(junctionDotWorld);
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }

        // draw vehicles
        final double VEHICLE_LENGTH = 4.5; 
        final double VEHICLE_WIDTH = 1.5;
        final double VEHICLE_LENGTH_PX = transform.worldscreenSize(VEHICLE_LENGTH);
        final double VEHICLE_WIDTH_PX = transform.worldscreenSize(VEHICLE_WIDTH);

        for (VehicleData vehicle : vehicleDataList) { // draw each vehicle 
            double screenX = transform.worldscreenX(vehicle.x);
            double screenY = transform.worldscreenY(vehicle.y);
            


            // Draw rectangle for vehicle
            g.setFill(vehicle.color);
            g.fillRect(
                screenX - VEHICLE_LENGTH_PX / 2.0,
                screenY - VEHICLE_WIDTH_PX / 2.0,
                VEHICLE_LENGTH_PX,
                VEHICLE_WIDTH_PX
            );
        }

        // draw traffic lights (mỗi ký tự trong lightDef là một bóng đèn tại junction)
        if (simulationWrapper != null) {
            final double TL_SIZE = 1.0;
            final double TL_SIZE_PX = transform.worldscreenSize(TL_SIZE);
            final double GAP_PX = TL_SIZE_PX * 1.2;   // khoảng cách giữa các đèn
            final int LIGHTS_PER_ROW = 4;             // số đèn trên một hàng

            List<String> tlIDs = simulationWrapper.getTLIDsList();
            for (String tlID : tlIDs) {
                String lightDef = simulationWrapper.getTLPhaseDef(tlID);

                // tìm junction tương ứng để lấy vị trí
                for (Networkpaser.Junction j : model.junctions) {
                    if (!j.id.equals(tlID)) continue;

                    double baseX = transform.worldscreenX(j.x);
                    double baseY = transform.worldscreenY(j.y);

                    if (lightDef != null && !lightDef.isEmpty()) {
                        for (int idx = 0; idx < lightDef.length(); idx++) {
                            char state = lightDef.charAt(idx);
                            Color tlColor;
                            if (state == 'r' || state == 'R') tlColor = Color.RED;
                            else if (state == 'y' || state == 'Y') tlColor = Color.YELLOW;
                            else if (state == 'g' || state == 'G') tlColor = Color.GREEN;
                            else tlColor = Color.GRAY;

                            int col = idx % LIGHTS_PER_ROW;
                            int row = idx / LIGHTS_PER_ROW;
                            double offsetX = (col - (LIGHTS_PER_ROW - 1) / 2.0) * GAP_PX;
                            double offsetY = -row * GAP_PX; // xếp các hàng lên trên
                            double cx = baseX + offsetX;
                            double cy = baseY + offsetY;

                            g.setFill(tlColor);
                            g.fillOval(cx - TL_SIZE_PX / 2.0, cy - TL_SIZE_PX / 2.0, TL_SIZE_PX, TL_SIZE_PX);
                            g.setStroke(Color.BLACK);
                            g.setLineWidth(1);
                            g.strokeOval(cx - TL_SIZE_PX / 2.0, cy - TL_SIZE_PX / 2.0, TL_SIZE_PX, TL_SIZE_PX);
                        }
                    }
                    break; // đã tìm thấy junction này, sang tlID khác
                }
            }
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
