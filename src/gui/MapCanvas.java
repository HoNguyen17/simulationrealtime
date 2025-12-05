package gui; 
import paser.Networkpaser;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

public class MapCanvas {
    private final Canvas canvas;
    private final GraphicsContext g;

    private Networkpaser.NetworkModel model;

    private double scale = 1.0;
    private double offsetX = 600;
    private double offsetY = 400;
    private double lastDragX = 0, lastDragY = 0;

    public MapCanvas(double w, double h) {
        canvas = new Canvas(w, h);
        g = canvas.getGraphicsContext2D();

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

    public void render() {
        if (model == null) return;
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (Networkpaser.Edge e : model.edges){
            boolean internal = e.id.startsWith(":");
            g.setStroke(internal ? Color.RED : Color.GREEN);
            g.setLineWidth(internal ? 6.0 : 7.0);

            for (Networkpaser.Lane lane : e.lanes){
                if (lane.shapePoints.size() < 2) continue;
                for(int i = 1; i < lane.shapePoints.size(); i++){
                    double x1 = lane.shapePoints.get(i - 1).getX() * scale + offsetX;
                    double y1 = lane.shapePoints.get(i - 1).getY() * scale + offsetY;
                    double x2 = lane.shapePoints.get(i).getX() * scale + offsetX;
                    double y2 = lane.shapePoints.get(i).getY() * scale + offsetY;
                    g.strokeLine(x1, y1, x2, y2);
                }
            }
        }

        g.setFill(Color.ORANGE);
        for (Networkpaser.Junction j : model.junctions) {
            double x = j.x * scale + offsetX;
            double y = j.y * scale + offsetY;
            g.fillOval(x - 2, y - 2, 4, 4);
        }
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
