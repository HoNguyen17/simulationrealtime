package gui;

import javafx.scene.canvas.Canvas;
import paser.Networkpaser;

//Manages view transformations: zoom, pan, and coordinate system

public class View {
    private final Canvas canvas;
    private Transform transform;
    private final Networkpaser.NetworkModel network;

    // View transformation state
    private double scale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double zoom = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    // Mouse drag state
    private double dragStartX, dragStartY, dragStartPanX, dragStartPanY;

    public View(Canvas canvas, Transform transform, Networkpaser.NetworkModel network) {
        this.canvas = canvas;
        this.transform = transform;
        this.network = network;
        updateTransform();
    }

    //Fit entire network to canvas with margins and reset zoom/pan 
    public void resetView() {
        double margin = 60.0; // Margin in screen pixels
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        if (network != null) {
            for (paser.Networkpaser.Edge e : network.edges) {
                for (paser.Networkpaser.Lane l : e.lanes) {
                    for (javafx.geometry.Point2D p : l.shapePoints) {
                        if (p.getX() < minX) minX = p.getX();
                        if (p.getY() < minY) minY = p.getY();
                        if (p.getX() > maxX) maxX = p.getX();
                        if (p.getY() > maxY) maxY = p.getY();
                    }
                }
            }
            if (!Double.isFinite(minX)) {
                for (Networkpaser.Junction j : network.junctions) {
                    if (j.x < minX) minX = j.x;
                    if (j.y < minY) minY = j.y;
                    if (j.x > maxX) maxX = j.x;
                    if (j.y > maxY) maxY = j.y;
                }
            }
        }

        if (!Double.isFinite(minX)) {
            scale = 1.0;
            offsetX = offsetY = 0.0;
            zoom = 1.0;
            panX = panY = 0.0;
            updateTransform();
            return;
        }

        double netW = maxX - minX;
        double netH = maxY - minY;
        if (netW == 0 || netH == 0) {
            scale = 1.0;
            offsetX = offsetY = 0.0;
            zoom = 1.0;
            panX = panY = 0.0;
            updateTransform();
            return;
        }

        // Calculate scale to fit network with margins, maintain aspect ratio
        scale = Math.min((canvas.getWidth() - 2 * margin) / netW,
                (canvas.getHeight() - 2 * margin) / netH);

        // Align so that min point appears with margin, and center residual
        offsetX = margin - minX * scale;
        offsetY = margin - minY * scale;

        // Reset user modifications
        zoom = 1.0;
        panX = panY = 0.0;
        updateTransform();
    }

    //Zoom to screen center (for buttons) 
    public void zoomcenter(double factor) {
        double centerX = canvas.getWidth() / 2.0;
        double centerY = canvas.getHeight() / 2.0;
        zoompoint(factor, centerX, centerY);
    }

    //Zoom to specific point (for scroll wheel at cursor) 
    public void zoompoint(double factor, double targetX, double targetY) {
        double worldX = transform.screenworldX(targetX);
        double worldY = transform.screenworldY(targetY);

        // Apply new zoom level
        zoom = Math.max(0.1, Math.min(10.0, zoom * factor));
        updateTransform();

        // Calculate where that world point appears now
        double newScreenX = transform.worldscreenX(worldX);
        double newScreenY = transform.worldscreenY(worldY);

        // Adjust pan to keep the world point under the cursor
        panX += (targetX - newScreenX);
        panY += (targetY - newScreenY);

        updateTransform();
    }

    //Startpanting operation
    public void startPan(double screenX, double screenY) {
        dragStartX = screenX;
        dragStartY = screenY;
        dragStartPanX = panX;
        dragStartPanY = panY;
    }

    // Update panning operation
    public void updatePan(double screenX, double screenY) {
        panX = dragStartPanX + (screenX - dragStartX);
        panY = dragStartPanY + (screenY - dragStartY);
        updateTransform();
    }

    // Update the coordinate transform with current view state 
    public void updateTransform() {
        if (transform != null) {
            transform.updateTransform(scale, offsetX, offsetY, zoom, panX, panY);
        }
    }

    //Update transform reference (needed when canvas resizes)
    public void setTransform(Transform transform) {
        this.transform = transform;
        updateTransform();
    }

    //Getter transform
    public Transform getTransform() {
        return transform;
    }
}
