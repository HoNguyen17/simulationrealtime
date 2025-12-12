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

    // Helper method để vẽ một cột đèn giao thông với 3 bóng (red, yellow, green)
    private void drawTrafficLightPole(GraphicsContext g, double centerX, double centerY, 
                                      double bulbSize, char state) {
        final double SPACING = bulbSize * 1.3; // khoảng cách giữa các bóng
        final double POLE_WIDTH = bulbSize * 0.15; // độ rộng cột đèn
        
        // Vẽ cột đèn (màu đen/xám)
        g.setFill(Color.web("#333333"));
        g.fillRect(centerX - POLE_WIDTH / 2.0, centerY - SPACING * 1.5, 
                   POLE_WIDTH, SPACING * 3);
        
        // Normalize state về lowercase để so sánh
        char normalizedState = Character.toLowerCase(state);
        
        // Vẽ 3 bóng đèn từ trên xuống: Red, Yellow, Green
        double[] bulbY = {
            centerY - SPACING,      // Red (trên cùng)
            centerY,                 // Yellow (giữa)
            centerY + SPACING        // Green (dưới cùng)
        };
        
        Color[] bulbColors = {Color.RED, Color.YELLOW, Color.GREEN};
        char[] bulbStates = {'r', 'y', 'g'};
        
        for (int i = 0; i < 3; i++) {
            // Xác định màu: sáng nếu match state, tắt nếu không
            Color bulbColor;
            if (normalizedState == bulbStates[i]) {
                bulbColor = bulbColors[i]; // Bóng sáng với màu tương ứng
            } else {
                bulbColor = Color.web("#444444"); // Tắt (màu xám đậm)
            }
            
            // Vẽ bóng đèn
            g.setFill(bulbColor);
            g.fillOval(centerX - bulbSize / 2.0, bulbY[i] - bulbSize / 2.0, bulbSize, bulbSize);
            
            // Vẽ viền bóng đèn
            g.setStroke(Color.BLACK);
            g.setLineWidth(1);
            g.strokeOval(centerX - bulbSize / 2.0, bulbY[i] - bulbSize / 2.0, bulbSize, bulbSize);
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

        // draw traffic lights (cột đèn với 3 bóng ở đầu mỗi lane đi vào junction)
        // 2 đèn dọc và 2 đèn ngang có màu trái ngược nhau, cố định theo phase
        if (simulationWrapper != null) {
            final double TL_BULB_SIZE = 0.6; // kích thước mỗi bóng đèn
            final double TL_BULB_SIZE_PX = transform.worldscreenSize(TL_BULB_SIZE);

            List<String> tlIDs = simulationWrapper.getTLIDsList();
            
            for (String tlID : tlIDs) {
                // Lấy phase number để xác định màu
                int phaseNum = simulationWrapper.getTLPhaseNum(tlID);
                
                // Kiểm tra phaseNum hợp lệ
                if (phaseNum < 0) continue;
                
                // Tìm junction tương ứng
                Networkpaser.Junction targetJunction = null;
                for (Networkpaser.Junction j : model.junctions) {
                    if (j.id.equals(tlID)) {
                        targetJunction = j;
                        break;
                    }
                }
                
                if (targetJunction == null) continue;

                // Tìm tất cả các edge đi vào junction này (edge.to == junction.id)
                List<Networkpaser.Lane> incomingLanes = new ArrayList<>();
                for (Networkpaser.Edge e : model.edges) {
                    if (e.to != null && e.to.equals(tlID)) {
                        incomingLanes.addAll(e.lanes);
                    }
                }

                if (incomingLanes.isEmpty()) continue;

                // Phân loại lanes thành dọc (vertical) và ngang (horizontal)
                List<Networkpaser.Lane> verticalLanes = new ArrayList<>();
                List<Networkpaser.Lane> horizontalLanes = new ArrayList<>();
                
                for (Networkpaser.Lane lane : incomingLanes) {
                    if (lane.shapePoints.size() < 2) continue;
                    
                    Point2D start = lane.shapePoints.get(0);
                    Point2D end = lane.shapePoints.get(lane.shapePoints.size() - 1);
                    double dx = end.getX() - start.getX();
                    double dy = end.getY() - start.getY();
                    
                    if (Math.abs(dy) > Math.abs(dx)) {
                        verticalLanes.add(lane);
                    } else {
                        horizontalLanes.add(lane);
                    }
                }

                // Xác định màu dựa trên phase
                // Chu kỳ: green -> yellow -> red -> yellow -> green...
                char verticalState, horizontalState;
                int phaseMod = phaseNum % 4;
                
                if (phaseMod == 0) {
                    // Phase 0: dọc green, ngang red
                    verticalState = 'g';
                    horizontalState = 'r';
                } else if (phaseMod == 1) {
                    // Phase 1: dọc yellow (chuyển từ green sang red), ngang red
                    verticalState = 'y';
                    horizontalState = 'r';
                } else if (phaseMod == 2) {
                    // Phase 2: dọc red, ngang green
                    verticalState = 'r';
                    horizontalState = 'g';
                } else {
                    // Phase 3: dọc red, ngang yellow (chuyển từ green sang red)
                    verticalState = 'r';
                    horizontalState = 'y';
                }

                // Vẽ đèn cho lanes dọc
                for (Networkpaser.Lane lane : verticalLanes) {
                    if (lane.shapePoints.isEmpty()) continue;
                    
                    Point2D laneEnd = lane.shapePoints.get(lane.shapePoints.size() - 1);
                    double screenX = transform.worldscreenX(laneEnd.getX());
                    double screenY = transform.worldscreenY(laneEnd.getY());
                    
                    drawTrafficLightPole(g, screenX, screenY, TL_BULB_SIZE_PX, verticalState);
                }

                // Vẽ đèn cho lanes ngang
                for (Networkpaser.Lane lane : horizontalLanes) {
                    if (lane.shapePoints.isEmpty()) continue;
                    
                    Point2D laneEnd = lane.shapePoints.get(lane.shapePoints.size() - 1);
                    double screenX = transform.worldscreenX(laneEnd.getX());
                    double screenY = transform.worldscreenY(laneEnd.getY());
                    
                    drawTrafficLightPole(g, screenX, screenY, TL_BULB_SIZE_PX, horizontalState);
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
