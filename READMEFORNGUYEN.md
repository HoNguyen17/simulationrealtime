# SUMO Real‑Time Simulation Dashboard (JavaFX)

This project is a JavaFX application that visualizes and interacts with a running SUMO (Simulation of Urban Mobility) traffic simulation in real time. It renders the road network, vehicles, and traffic light states, and provides pan/zoom controls and a side dashboard.

The application entrypoint is App.java and the main UI canvas is in gui/MapCanvas.java.

## Highlights
- Real‑time SUMO stepping via wrapper classes (wrapper/SimulationWrapper, VehicleWrapper, TrafficLightWrapper).
- JavaFX rendering of network geometry, vehicles, and traffic light bars.
- Responsive pan/zoom with smooth redraws.
- Configurable network and simulation inputs from resource/.

## Project Structure
- src/: Java sources
	- App.java: JavaFX Application entrypoint; loads network, starts simulation, and wires UI
	- gui/: UI components (MapCanvas, Dashboard, View, Transform)
	- paser/: Network parser (Networkpaser) for SUMO .net.xml
	- wrapper/: SUMO TraCI wrappers (SimulationWrapper, VehicleWrapper, TrafficLightWrapper)
- resource/: SUMO configs and sample scenarios (*.net.xml, *.rou.xml, *.sumocfg)
- lib/: Third‑party libraries (JavaFX, TraaS/TraCI client; adjust as needed)
- bin/: Compiled classes (VS Code default)

## OOP Design (Java)
- Encapsulation: MapCanvas encapsulates rendering state and logic (sprites, transforms, event handling). Inner classes VehicleSprite and TrafficLightSprite keep per‑entity state private to the canvas.
- Composition: MapCanvas composes Canvas, GraphicsContext, Transform, and View to build behavior; the app composes Dashboard + MapCanvas in a BorderPane.
- Abstraction: Wrapper classes in wrapper/ abstract TraCI calls (simulation step, vehicle positions/angles/colors, TL phases/links) behind simple methods used by the UI.
- Inheritance: App extends javafx.application.Application to integrate with the JavaFX lifecycle (start(), launch()).
- Data Records: Lightweight immutable data carriers MapCanvas.VehicleData and MapCanvas.TrafficLightData model per‑frame inputs to the renderer.
- Immutability & State Management: Rendered geometry is derived from Networkpaser.NetworkModel; dynamic entities are updated per tick and reconciled to sprite maps.

## Requirements
- Java 17+ (recommended)
- JavaFX SDK matching your Java version (on classpath/module‑path)
- SUMO installed, with TraCI enabled
- TraaS (TraCI Java client) or equivalent JARs available in lib/

## Configure Inputs
By default, App.java uses:
- Network: resource/test_7_huge.net.xml
- Config: resource/test_7_huge.sumocfg

These are set in App as:
- NET_FILE = "..\\resource\\test_7_huge.net.xml"
- SUMOCFG_FILE = "..\\resource\\test_7_huge.sumocfg"

You can change them to any scenario files in resource/.

## Run the App
1. Ensure JavaFX and TraaS JARs are available (e.g., in lib/). On Windows, you typically need to pass JavaFX modules on the module‑path.
2. From VS Code, run App.java. Or use the terminal:

```powershell
# Example using classpath; adjust JavaFX paths for your setup
$javafx = "C:\\path\\to\\javafx-sdk-XX\\lib"
$classpath = "lib\\*;bin"  # VS Code compiles to bin

java `
	--module-path "$javafx" `
	--add-modules javafx.controls,javafx.graphics `
	-cp $classpath `
	App
```

If you prefer sumocfg execution via SUMO GUI/CLI, ensure SUMO runs with TraCI server settings compatible with the wrappers.

## How It Works
- App.start():
	- Parses network via Networkpaser.parse(NET_FILE).
	- Creates MapCanvas, sets model, fits view, renders.
	- Starts SUMO via SimulationWrapper(SUMOCFG_FILE) and a background stepping Thread.
	- Uses a JavaFX AnimationTimer to poll vehicle/TL data and update MapCanvas each frame.
	- Constructs UI (Dashboard on the left, MapCanvas center) and handles clean shutdown on window close.

## Troubleshooting
- Missing JavaFX: add SDK to --module-path and --add-modules.
- SUMO not stepping: verify SUMO installed and *.sumocfg paths valid; check TraCI port settings in wrappers.
- Rendering issues: confirm resource/ files exist and network parses without errors.

## License
This repository includes third‑party resources (SUMO configs and libraries). Ensure you comply with their respective licenses.
