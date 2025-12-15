package gui;

/*
Transform encapsulates coordinate transformations 
between world coordinates and screen coordinates.Holds state
for scale, offset, zoom, and pan.
*/
public class Transform {
    private double scale, offsetX, offsetY, zoom, panX, panY;
    protected  double canvasHeight;


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

    // Update all transform parameters
    public void updateTransform(double scale, double offsetX, double offsetY, double zoom, double panX, double panY) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
    }

    // Update canvas height (for Y-axis inversion)
    public void setCanvasHeight(double canvasHeight) {
        this.canvasHeight = canvasHeight;
    }
    // World to screen coordinate conversions
    public double worldscreenX(double worldX) {
        return (worldX * scale * zoom) + offsetX + panX;
    }
    // Invert Y-axis for screen coordinates
    public double worldscreenY(double worldY) {
        return (worldY * scale * zoom) + offsetY + panY;
    }

    // Screen to world coordinate conversions
    public double screenworldX(double screenX) {
        return (screenX - offsetX - panX) / (scale * zoom);
    }
    // Invert Y-axis for screen coordinates
    public double screenworldY(double screenY) {
        return (screenY - offsetY - panY) / (scale * zoom);
    }


    // Convert world size to screen size
    public double worldscreenSize(double worldSize) {
        return worldSize * scale * zoom;
    }
}