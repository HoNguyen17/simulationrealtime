
package gui;

import paser.Networkpaser;
import wrapper.SimulationWrapper;

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
    //...
    private final Map<String, TrafficLightSprite> trafficLightSprites = new HashMap<>();
    private List<TrafficLightData> trafficLightDataList = new ArrayList<>();
    private SimulationWrapper simulationWrapper;

    protected  double lastDragX = 0, lastDragY = 0; // last mouse drag positions

    public static record VehicleData(String id, double x, double y, double angle, Color color) {}
    //...
    // states[i] applies to the controlled link from[i] -> to[i]
    public static record TrafficLightData(String id, double x, double y, List<Character> states, List<String> fromLaneIds, List<String> toLaneIds) {}




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
    //...

    private static class TrafficLightSprite {
        final String id;
        double worldX, worldY;
        List<Character> states; // r/y/g for each controlled link

        TrafficLightSprite(String id, double x, double y, List<Character> states) {
            this.id = id;
            this.worldX = x;
            this.worldY = y;
            this.states = new ArrayList<>(states);
        }

        void update(double x, double y, List<Character> states) {
            this.worldX = x;
            this.worldY = y;
            this.states = new ArrayList<>(states);
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


    //...
    // Set traffic light data for rendering (call from wrapper)
    public void setTrafficLightData(List<TrafficLightData> trafficLights) {
        this.trafficLightDataList = trafficLights != null ? trafficLights : List.of();
        for (TrafficLightData tl : this.trafficLightDataList) {
            TrafficLightSprite sprite = trafficLightSprites.get(tl.id());
            if (sprite == null) {
                sprite = new TrafficLightSprite(tl.id(), tl.x(), tl.y(), tl.states());
                trafficLightSprites.put(tl.id(), sprite);
            } else {
                sprite.update(tl.x(), tl.y(), tl.states());
            }
        }
        trafficLightSprites.keySet().removeIf(id ->
                trafficLightDataList.stream().noneMatch(tl -> tl.id().equals(id))
        );
    }

    public void setSimulationWrapper(SimulationWrapper simulationWrapper) {
        this.simulationWrapper = simulationWrapper;
        // Don't render immediately - wait for traffic lights to be initialized
        // Traffic lights will be rendered in the animation timer loop
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

        //...

        // draw traffic lights as 4-cell horizontal row at the end of each incoming lane
        if (simulationWrapper != null) {
            final double CELL_SIZE = transform.worldscreenSize(0.6); // Cell size (slightly bigger)
            final double CELL_SPACING = transform.worldscreenSize(0.08); // Spacing between cells
            final double OFFSET_FROM_LANE_END = transform.worldscreenSize(0.2); // Small offset to stick to lane end

            List<String> tlIDs = simulationWrapper.getTLIDsList();
            if (tlIDs == null) return;
            
            for (String tlID : tlIDs) {
                // Check if traffic light has phase definition (lightDef) initialized
                String phaseDef = simulationWrapper.getTLPhaseDef(tlID);
                if (phaseDef == null || phaseDef.isEmpty()) {
                    continue; // Skip if not initialized yet
                }
                
                int controlledLinksNum = simulationWrapper.getTLControlledLinksNum(tlID);
                if (controlledLinksNum <= 0) continue;
                
                // Build map of all lanes for lookup
                Map<String, Networkpaser.Lane> allLanesMap = new HashMap<>();
                for (Networkpaser.Edge e : model.edges) {
                    for (Networkpaser.Lane lane : e.lanes) {
                        allLanesMap.put(lane.id, lane);
                    }
                }

                // Group controlled links by fromLaneId (each incoming lane gets its own 4-cell row)
                Map<String, Map<DirectionType, Character>> laneDirectionStates = new HashMap<>();

                // Process each controlled link to determine direction and state
                for (int idx = 0; idx < controlledLinksNum; idx++) {
                    List<String> defFromTo = simulationWrapper.getTLDefFromTo(tlID, idx);
                    if (defFromTo == null || defFromTo.size() < 3) continue;
                    
                    String stateStr = defFromTo.get(0);
                    String fromLaneId = defFromTo.get(1);
                    String toLaneId = defFromTo.get(2);
                    
                    if (stateStr == null || stateStr.isEmpty()) continue;
                    
                    char state = Character.toLowerCase(stateStr.charAt(0));
                    
                    // Get from and to lanes
                    Networkpaser.Lane fromLane = allLanesMap.get(fromLaneId);
                    Networkpaser.Lane toLane = allLanesMap.get(toLaneId);
                    
                    if (fromLane == null || fromLane.shapePoints.size() < 2) continue;
                    if (toLane == null || toLane.shapePoints.size() < 2) continue;
                    
                    // Initialize direction states map for this fromLane if not exists
                    if (!laneDirectionStates.containsKey(fromLaneId)) {
                        Map<DirectionType, Character> directionStates = new HashMap<>();
                        directionStates.put(DirectionType.U_TURN, null);
                        directionStates.put(DirectionType.LEFT, null);
                        directionStates.put(DirectionType.STRAIGHT, null);
                        directionStates.put(DirectionType.RIGHT, null);
                        laneDirectionStates.put(fromLaneId, directionStates);
                    }
                    
                    Map<DirectionType, Character> directionStates = laneDirectionStates.get(fromLaneId);
                    
                    // Calculate turn direction
                    Point2D fromEnd = fromLane.shapePoints.get(fromLane.shapePoints.size() - 1);
                    Point2D fromBefore = fromLane.shapePoints.get(fromLane.shapePoints.size() - 2);
                    Point2D toStart = toLane.shapePoints.get(0);
                    Point2D toNext = toLane.shapePoints.get(1);
                    
                    double fromDirX = fromEnd.getX() - fromBefore.getX();
                    double fromDirY = fromEnd.getY() - fromBefore.getY();
                    double fromDirLen = Math.hypot(fromDirX, fromDirY);
                    if (fromDirLen == 0) continue;
                    fromDirX /= fromDirLen;
                    fromDirY /= fromDirLen;
                    
                    double toDirX = toNext.getX() - toStart.getX();
                    double toDirY = toNext.getY() - toStart.getY();
                    double toDirLen = Math.hypot(toDirX, toDirY);
                    if (toDirLen == 0) continue;
                    toDirX /= toDirLen;
                    toDirY /= toDirLen;
                    
                    // Calculate angle between from and to directions
                    double dot = fromDirX * toDirX + fromDirY * toDirY;
                    double cross = fromDirX * toDirY - fromDirY * toDirX;
                    double turnAngle = Math.atan2(cross, dot) * 180.0 / Math.PI;
                    
                    // Normalize angle to -180 to 180
                    while (turnAngle > 180) turnAngle -= 360;
                    while (turnAngle < -180) turnAngle += 360;
                    
                    // Determine direction type
                    DirectionType dirType;
                    if (turnAngle < -135 || turnAngle > 135) {
                        dirType = DirectionType.U_TURN;
                    } else if (turnAngle < -45) {
                        dirType = DirectionType.LEFT;
                    } else if (turnAngle > 45) {
                        dirType = DirectionType.RIGHT;
                    } else {
                        dirType = DirectionType.STRAIGHT;
                    }
                    
                    // Store state for this direction (prefer green if available)
                    if (directionStates.get(dirType) == null || state == 'g') {
                        directionStates.put(dirType, state);
                    }
                }
                
                // Draw traffic light row for each incoming lane
                for (Map.Entry<String, Map<DirectionType, Character>> entry : laneDirectionStates.entrySet()) {
                    String fromLaneId = entry.getKey();
                    Map<DirectionType, Character> directionStates = entry.getValue();
                    
                    Networkpaser.Lane fromLane = allLanesMap.get(fromLaneId);
                    if (fromLane == null || fromLane.shapePoints.size() < 2) continue;
                    
                    // Get position at end of lane
                    Point2D laneEnd = fromLane.shapePoints.get(fromLane.shapePoints.size() - 1);
                    Point2D laneBeforeEnd = fromLane.shapePoints.get(fromLane.shapePoints.size() - 2);
                    
                    // Calculate lane direction
                    double dirX = laneEnd.getX() - laneBeforeEnd.getX();
                    double dirY = laneEnd.getY() - laneBeforeEnd.getY();
                    double dirLen = Math.hypot(dirX, dirY);
                    if (dirLen == 0) continue;
                    dirX /= dirLen;
                    dirY /= dirLen;
                    
                    // Position: stick to lane end (minimal offset)
                    double centerX = laneEnd.getX() - dirX * OFFSET_FROM_LANE_END;
                    double centerY = laneEnd.getY() - dirY * OFFSET_FROM_LANE_END;
                    
                    // Convert to screen coordinates
                    double screenX = transform.worldscreenX(centerX);
                    double screenY = transform.worldscreenY(centerY);
                    
                    // Calculate lane angle for rotation (in degrees, screen coordinates)
                    // JavaFX: 0° = right, 90° = down, -90° = up
                    double laneAngle = Math.atan2(dirY, dirX) * 180.0 / Math.PI;
                    
                    // Rotate 90 degrees to make frame horizontal (perpendicular) to lane direction
                    // This makes the frame "nằm ngang so với lane"
                    double frameAngle = laneAngle + 90.0;
                    
                    // Draw 4 cells in a row, rotated to be horizontal (perpendicular) to lane
                    // Order: U-turn - Left - Straight - Right (from left to right in frame)
                    drawTrafficLightRow(g, screenX, screenY, directionStates, CELL_SIZE, CELL_SPACING, frameAngle);
                }
            }
        }
    }

    private Networkpaser.Lane findLaneById(String laneId) {
        if (laneId == null || model == null) return null;
        for (Networkpaser.Edge e : model.edges) {
            for (Networkpaser.Lane l : e.lanes) {
                if (laneId.equals(l.id)) return l;
            }
        }
        return null;
    }

    // Enum for direction types
    private enum DirectionType {
        U_TURN, LEFT, STRAIGHT, RIGHT
    }

    // Draw traffic light row with 4 cells in line (U-turn, Left, Straight, Right)
    // Rotates the ENTIRE 4-cell frame together to align with lane direction
    private void drawTrafficLightRow(GraphicsContext g, double centerX, double centerY,
                                     Map<DirectionType, Character> directionStates,
                                     double cellSize, double spacing,
                                     double laneAngle) {
        g.save(); // Save current graphics state
        
        // Translate to center position
        g.translate(centerX, centerY);
        
        // Rotate the ENTIRE coordinate system (this rotates the whole 4-cell frame together)
        g.rotate(laneAngle);
        
        // Now all drawing operations will be in the rotated coordinate system
        // Calculate total width of the 4-cell row
        double totalWidth = cellSize * 4 + spacing * 3;
        double startX = -totalWidth / 2; // Start from left side of the row
        double startY = -cellSize / 2;   // Center vertically
        
        // Draw 4 cells in a horizontal row (horizontal in the rotated coordinate system)
        // Order from left to right: U-turn, Left, Straight, Right
        double currentX = startX;
        
        // U-turn (leftmost in lane direction)
        drawTrafficLightCell(g, currentX, startY, cellSize, 
                            directionStates.get(DirectionType.U_TURN), "U");
        currentX += cellSize + spacing;
        
        // Left
        drawTrafficLightCell(g, currentX, startY, cellSize,
                            directionStates.get(DirectionType.LEFT), "L");
        currentX += cellSize + spacing;
        
        // Straight
        drawTrafficLightCell(g, currentX, startY, cellSize,
                            directionStates.get(DirectionType.STRAIGHT), "S");
        currentX += cellSize + spacing;
        
        // Right (rightmost in lane direction)
        drawTrafficLightCell(g, currentX, startY, cellSize,
                            directionStates.get(DirectionType.RIGHT), "R");
        
        g.restore(); // Restore graphics state (undo rotation and translation)
    }
    
    // Draw a single traffic light cell with color and direction indicator
    private void drawTrafficLightCell(GraphicsContext g, double x, double y, double size,
                                     Character state, String directionLabel) {
        // Determine color based on state
        Color cellColor;
        if (state == null) {
            cellColor = Color.GRAY; // No state available
        } else {
            switch (Character.toLowerCase(state)) {
                case 'r':
                    cellColor = Color.RED;
                    break;
                case 'y':
                    cellColor = Color.YELLOW;
                    break;
                case 'g':
                    cellColor = Color.LIMEGREEN;
                    break;
                default:
                    cellColor = Color.GRAY;
                    break;
            }
        }
        
        // Fill cell with color
        g.setFill(cellColor);
        g.fillRect(x, y, size, size);
        
        // Draw border
        g.setStroke(Color.BLACK);
        g.setLineWidth(0.5);
        g.strokeRect(x, y, size, size);
        
        // Draw direction symbol in center (smaller for compact display)
        double centerX = x + size / 2;
        double centerY = y + size / 2;
        double labelSize = size * 1; // Smaller symbol
        
        g.setFill(Color.WHITE);
        g.setStroke(Color.WHITE);
        g.setLineWidth(0.5);
        
        // Draw direction symbol
        switch (directionLabel) {
            case "U": // U-turn (curved arrow)
                drawUTurnSymbol(g, centerX, centerY, labelSize);
                break;
            case "L": // Left arrow
                drawLeftArrowSymbol(g, centerX, centerY, labelSize);
                break;
            case "S": // Straight arrow
                drawStraightArrowSymbol(g, centerX, centerY, labelSize);
                break;
            case "R": // Right arrow
                drawRightArrowSymbol(g, centerX, centerY, labelSize);
                break;
        }
    }
    
    // Draw U-turn symbol
    private void drawUTurnSymbol(GraphicsContext g, double x, double y, double size) {
        double radius = size * 0.3;
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI / 2 + (Math.PI * i / segments);
            double angle2 = Math.PI / 2 + (Math.PI * (i + 1) / segments);
            double x1 = x + radius * Math.cos(angle1);
            double y1 = y + radius * Math.sin(angle1);
            double x2 = x + radius * Math.cos(angle2);
            double y2 = y + radius * Math.sin(angle2);
            g.strokeLine(x1, y1, x2, y2);
        }
    }
    
    // Draw left arrow symbol
    private void drawLeftArrowSymbol(GraphicsContext g, double x, double y, double size) {
        double arrowSize = size * 0.4;
        double[] xPoints = {x - arrowSize, x, x};
        double[] yPoints = {y, y - arrowSize/2, y + arrowSize/2};
        g.fillPolygon(xPoints, yPoints, 3);
        g.fillRect(x, y - arrowSize/4, arrowSize * 0.6, arrowSize/2);
    }
    
    // Draw straight arrow symbol
    private void drawStraightArrowSymbol(GraphicsContext g, double x, double y, double size) {
        double arrowSize = size * 0.4;
        double[] xPoints = {x, x - arrowSize/2, x + arrowSize/2};
        double[] yPoints = {y - arrowSize, y, y};
        g.fillPolygon(xPoints, yPoints, 3);
        g.fillRect(x - arrowSize/4, y, arrowSize/2, arrowSize * 0.6);
    }
    
    // Draw right arrow symbol
    private void drawRightArrowSymbol(GraphicsContext g, double x, double y, double size) {
        double arrowSize = size * 0.4;
        double[] xPoints = {x + arrowSize, x, x};
        double[] yPoints = {y, y - arrowSize/2, y + arrowSize/2};
        g.fillPolygon(xPoints, yPoints, 3);
        g.fillRect(x - arrowSize * 0.6, y - arrowSize/4, arrowSize * 0.6, arrowSize/2);
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
