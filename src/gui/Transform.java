package gui;

// This class is responsible for managing the coordinate system translations and scaling necessary to map a real-world coordinate system (like the one used by SUMO) onto the pixel-based screen of a JavaFX Canvas
public class Transform {
    private double scale, offsetX, offsetY, zoom, panX, panY;
    protected  double canvasHeight; // the height of the drawing surface
    // "scale": The initial scale factor, usually set to fit the whole world map into the canvas
    // "offsetX", "offsetY": Initial translation offsets to center the map based on the world coordinates

    // Initialize with canvas height for Y-axis inversion
    public Transform(double canvasHeight) {
        this.canvasHeight = canvasHeight;
        this.scale = 1.0;
        this.zoom = 1.0;
        this.offsetX = 0.0;
        this.offsetY = 0.0;
        this.panX = 0.0;
        this.panY = 0.0;
    }

    // provides a way to set all transformation parameters simultaneously. This is likely used by a separate view management class (View) to apply a calculated state change
    public void updateTransform(double scale, double offsetX, double offsetY, double zoom, double panX, double panY) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
    }

    // Update canvas height (for Y-axis inversion) -> allows the canvasHeight to be updated if the canvas is resized
    public void setCanvasHeight(double canvasHeight) {
        this.canvasHeight = canvasHeight;
    }
    // World to screen coordinate conversions
    // These methods convert coordinates from the SUMO world (real-world units, like meters) to the screen (pixel units)
    public double worldscreenX(double worldX) {
        return (worldX * scale * zoom) + offsetX + panX; // screenX = (worldX * scale * zoom) + offsetX + panX
    }
    // Invert Y-axis for screen coordinates
    public double worldscreenY(double worldY) {
        return (worldY * scale * zoom) + offsetY + panY;
    }

    // Screen to world coordinate conversions
    // These methods perform the inverse operation, converting screen coordinates (e.g., mouse click locations) back into world coordinates (e.g., to find what object the user clicked on)
    public double screenworldX(double screenX) {
        return (screenX - offsetX - panX) / (scale * zoom);
    }
    // Invert the transformation for the Y-axis for screen coordinates
    public double screenworldY(double screenY) {
        return (screenY - offsetY - panY) / (scale * zoom);
    }


    // Convert world size to screen size
    // Converts a distance or dimension (like vehicle length or lane width) from world units to the corresponding size in screen pixels. This ensures objects maintain their relative size visually when zooming
    public double worldscreenSize(double worldSize) {
        return worldSize * scale * zoom;
    }
}