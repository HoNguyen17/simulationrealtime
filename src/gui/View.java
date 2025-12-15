package gui;

import javafx.scene.canvas.Canvas;
import paser.Networkpaser;

// This class is responsible for managing the user's perspective or view state of the simulated traffic network
// It handles the logic for complex view manipulations like fitting the map, panning, and zooming by manipulating the parameters of the associated Transform object

public class View { // the "View" class is the control layer for the visualization pipeline
    private final Canvas canvas; // JavaFX drawing surface size (used for centering and fitting)
    private Transform transform; // the class that mathematically performs the coordinate conversion
    private final Networkpaser.NetworkModel network; // used to determine the initial bounds of the map

    // View transformation state -> store the core transformation parameters that define the current view. These mirror the parameters in the Transform class: scale, offsetX, offsetY, zoom, panX, and panY
    private double scale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double zoom = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    // Mouse drag state -> used to track the initial state when a mouse drag (panning) operation begins
    private double dragStartX, dragStartY, dragStartPanX, dragStartPanY;

    public View(Canvas canvas, Transform transform, Networkpaser.NetworkModel network) {
        this.canvas = canvas;
        this.transform = transform;
        this.network = network;
        updateTransform();
    }

    // Fit entire network to canvas with margins and reset zoom/pan
    public void resetView() { // calculate the initial transformation parameters to fit the entire network model within the canvas boundaries, including a small margin
        double margin = 60.0; // Margin in screen pixels
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        // Find Network Bounds: It iterates through all the shapePoints of all lanes and junctions in the network model to determine the global minimum and maximum world coordinates (minX, maxX, minY, maxY)
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

        // Calculate Initial Scale -> the scale is determined by finding the minimum ratio needed to fit the network width (netW) and height (netH) into the available canvas space minus a margin
        scale = Math.min((canvas.getWidth() - 2 * margin) / netW,
                (canvas.getHeight() - 2 * margin) / netH);

        // Align so that min point appears with margin, and center residual
        // Calculate Initial Offset -> the offsetX and offsetY are calculated to place the minX, minY coordinate exactly at the screen's margin boundary
        offsetX = margin - minX * scale;
        offsetY = margin - minY * scale;

        // Reset user modifications
        zoom = 1.0;
        panX = panY = 0.0;
        updateTransform();
    }

    // Zoom to screen center (for buttons)
    public void zoomcenter(double factor) { // used for button-based zoom. It calculates the center of the canvas and calls zoompoint with that center
        double centerX = canvas.getWidth() / 2.0;
        double centerY = canvas.getHeight() / 2.0;
        zoompoint(factor, centerX, centerY);
    }

    // Zoom to specific point (for scroll wheel at cursor)
    public void zoompoint(double factor, double targetX, double targetY) { // designed for mouse wheel interaction, which zooms relative to the cursor (targetX, targetY)
        // Identify world point -> it first uses transform.screenworldX/Y() to find the world coordinate that is currently under the cursor
        double worldX = transform.screenworldX(targetX);
        double worldY = transform.screenworldY(targetY);

        // Apply new zoom level -> it updates the zoom level, clamping it between 0.1 and 10.0 to prevent excessive scaling
        zoom = Math.max(0.1, Math.min(10.0, zoom * factor));
        updateTransform();

        // Recalculate screen position -> it calculates where the previously identified world coordinate now appears on the screen (newScreenX/Y) after the zoom change
        double newScreenX = transform.worldscreenX(worldX);
        double newScreenY = transform.worldscreenY(worldY);

        // Adjust pan -> the difference between the original cursor position (targetX/Y) and the new screen position is the distance the view needs to shift (pan) to keep the world point fixed under the cursor. This creates the "zoom to cursor" effect
        panX += (targetX - newScreenX);
        panY += (targetY - newScreenY);

        updateTransform();
    }

    // Start panning operation -> record the mouse position and the current panX/Y values
    public void startPan(double screenX, double screenY) {
        dragStartX = screenX;
        dragStartY = screenY;
        dragStartPanX = panX;
        dragStartPanY = panY;
    }

    // Update panning operation -> calculate the change in mouse position since startPan and adds that delta to the initial pan coordinates (dragStartPanX/Y), then updating the current panX/Y in real-time
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

    //Update transform reference (needed when canvas resizes) -> ensure that any changes calculated in the View state (scale, zoom, panX, ...) are immediately synchronized with the separate Transform object, which is what the MapCanvas uses for rendering
    public void setTransform(Transform transform) {
        this.transform = transform;
        updateTransform();
    }

    //Getter transform
    public Transform getTransform() {
        return transform;
    }
}