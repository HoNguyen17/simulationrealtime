package gui;

public class HotspotData {
    private final String id;
    private final double speed;

    public HotspotData(String id, double speed) {
        this.id = id;
        this.speed = speed;
    }

    public String getId() { return id; }
    public double getSpeed() { return Math.round(speed * 100.0) / 100.0; }
}
