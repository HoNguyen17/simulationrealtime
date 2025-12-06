package gui; 
import paser.Networkpaser;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import wrapper.SimulationWrapper;

public class MapCanvas {
    private final Canvas canvas;
    private final GraphicsContext g;

    private Networkpaser.NetworkModel model;
    private VehicleRenderer vehicleRenderer;

    private double scale = 1.0;
    private double offsetX = 600;
    private double offsetY = 400;
    private double lastDragX = 0, lastDragY = 0;


    public MapCanvas(double w, double h) {
        canvas = new Canvas(w, h);
        g = canvas.getGraphicsContext2D();
        vehicleRenderer = new VehicleRenderer();

        // Pan
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastDragX = e.getX();
            lastDragY = e.getY();
        });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = e.getX() - lastDragX;
            double dy = e.getY() - lastDragY;
            offsetX += dx;
            offsetY += dy;
            lastDragX = e.getX();
            lastDragY = e.getY();
            render();
        });

        // Scroll zoom
        canvas.addEventHandler(ScrollEvent.SCROLL, e -> {
            double mouseX = e.getX();
            double mouseY = e.getY();
            double oldScale = scale;
            double factor = (e.getDeltaY() > 0) ? 1.1 : 0.9;
            scale = clamp(scale * factor, 0.1, 50.0);

            double worldX = (mouseX - offsetX) / oldScale;
            double worldY = (mouseY - offsetY) / oldScale;
            offsetX = mouseX - worldX * scale;
            offsetY = mouseY - worldY * scale;

            render();
        });
    }

    public Canvas getCanvas() { return canvas; }
    public void setModel(Networkpaser.NetworkModel model) { this.model = model; }
    
    /**
     * Lấy VehicleRenderer để quản lý xe
     */
    public VehicleRenderer getVehicleRenderer() {
        return vehicleRenderer;
    }
    
    /**
     * Cập nhật xe từ SimulationWrapper
     */
    public void updateVehiclesFromSimulation(SimulationWrapper sim) {
        vehicleRenderer.updateFromSimulation(sim);
    }
    
    /**
     * Xóa tất cả xe
     */
    public void clearVehicles() {
        vehicleRenderer.clear();
    }

    // Simple offset of a polyline by distance d (screen space, after scaling).
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
            // normal vector
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
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 0) Fill junction polygons trước để tạo nền giao cắt liền mạch
        Color roadFill = Color.web("#2b2b2b");
        g.setFill(roadFill);
        for (Networkpaser.Junction j : model.junctions) {
            if (j.shapePoints == null || j.shapePoints.size() < 3) continue;
            // chuyển sang tọa độ màn hình
            double[] xs = new double[j.shapePoints.size()];
            double[] ys = new double[j.shapePoints.size()];
            for (int i = 0; i < j.shapePoints.size(); i++) {
                Point2D p = j.shapePoints.get(i);
                xs[i] = p.getX() * scale + offsetX;
                ys[i] = p.getY() * scale + offsetY;
            }
            g.fillPolygon(xs, ys, xs.length);
        }
        // edit draw roads
        double roadHalfWidth = 20.0;      // nửa bề rộng nền đường
        double centerMarkWidth = 3.0;    // độ dày vạch vàng giữa
        //double sideMarkWidth   = 2.0;    // độ dày vạch trắng
        //double laneOffset      = 5.0;    // khoảng lệch cho vạch trắng hai bên
        for (Networkpaser.Edge e : model.edges){
            if (e.id.startsWith(":")) continue;

            for (Networkpaser.Lane lane : e.lanes){
                if (lane.shapePoints.size() < 2) continue;

                // Build screen-space polyline
                List<Point2D> screenPts = new ArrayList<>();
                for (Point2D p : lane.shapePoints) {
                    screenPts.add(new Point2D(p.getX() * scale + offsetX, p.getY() * scale + offsetY));
                }

                // 1) Nền đường (xám đậm, nét liền, dày)
                g.setStroke(roadFill);
                g.setLineWidth(roadHalfWidth * 2);
                g.setLineDashes(); // solid
                drawPolyline(g, screenPts);

                // 2) Vạch vàng giữa (nét đứt dài)
                 List<Point2D> centerMark = offsetPolyline(screenPts, 0.0);
                g.setStroke(Color.web("#ffd200"));
                g.setLineWidth(centerMarkWidth);
                g.setLineDashes(18, 12);
                drawPolyline(g, centerMark);

                // 3) Vạch trắng chia làn (hai bên, nét đứt ngắn hơn)
                //List<Point2D> leftMark  = offsetPolyline(screenPts, -laneOffset);
                //List<Point2D> rightMark = offsetPolyline(screenPts,  laneOffset);
                g.setStroke(Color.WHITE);
                //g.setLineWidth(sideMarkWidth);
                g.setLineDashes(12, 10);
                //drawPolyline(g, leftMark);
                //drawPolyline(g, rightMark);
            }
        }

        g.setFill(Color.ORANGE);
        for (Networkpaser.Junction j : model.junctions) {
            double x = j.x * scale + offsetX;
            double y = j.y * scale + offsetY;
            g.fillOval(x - 2, y - 2, 4, 4);
        }

        // Render vehicles sử dụng VehicleRenderer
        vehicleRenderer.render(g, scale, offsetX, offsetY);
    }

    public void fitAndCenter() {
        if (model == null) return;
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (Networkpaser.Edge e : model.edges){
            for (Networkpaser.Lane l : e.lanes){
                for (javafx.geometry.Point2D p : l.shapePoints){
                    if (p.getX() < minX) minX = p.getX();
                    if (p.getY() < minY) minY = p.getY();
                    if (p.getX() > maxX) maxX = p.getX();
                    if (p.getY() > maxY) maxY = p.getY();
                }
            }
        }
        if (!Double.isFinite(minX)) {
            for (Networkpaser.Junction j : model.junctions) {
                if (j.x < minX) minX = j.x;
                if (j.y < minY) minY = j.y;
                if (j.x > maxX) maxX = j.x;
                if (j.y > maxY) maxY = j.y;
            }
        }
        if (!Double.isFinite(minX)) return;

        double width = maxX - minX;
        double height = maxY - minY;
        double pad = 40.0;
        double scaleX = (canvas.getWidth() - 2 * pad) / (width <= 0 ? 1 : width);
        double scaleY = (canvas.getHeight() - 2 * pad) / (height <= 0 ? 1 : height);
        scale = Math.min(scaleX, scaleY);

        double centerMapX = (minX + maxX) / 2.0;
        double centerMapY = (minY + maxY) / 2.0;
        offsetX = canvas.getWidth() / 2.0 - centerMapX * scale;
        offsetY = canvas.getHeight() / 2.0 - centerMapY * scale;
    }

    public void zoomAtCenter(double factor) {
        double mouseX = canvas.getWidth() / 2.0;
        double mouseY = canvas.getHeight() / 2.0;
        double oldScale = scale;
        scale = clamp(scale * factor, 0.1, 50.0);
        double worldX = (mouseX - offsetX) / oldScale;
        double worldY = (mouseY - offsetY) / oldScale;
        offsetX = mouseX - worldX * scale;
        offsetY = mouseY - worldY * scale;
        render();
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
    
}
