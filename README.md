# 🚦 SUMO Real-Time Traffic Simulation & Analytics Dashboard

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![UI Framework](https://img.shields.io/badge/GUI-JavaFX_17-blue.svg)](https://openjfx.io/)
[![Simulation Engine](https://img.shields.io/badge/Simulation-Eclipse_SUMO_/_TraCI-green.svg)](https://eclipse.dev/sumo/)
[![Institution](https://img.shields.io/badge/Frankfurt_UAS-Wintersemester_2025%2F2026-red.svg)](https://www.frankfurt-university.de/)

A high-performance real-time urban traffic simulation visualization dashboard developed for the **Java Programming** course at **Frankfurt University of Applied Sciences** (Frankfurt UAS, Germany) in **Wintersemester 2025/2026**. 

The system parses Eclipse SUMO network topology files (`.net.xml`), connects dynamically to the SUMO simulation runtime using the **TraCI** protocol (`TraaS`), and provides interactive 2D map visualization, real-time vehicle and signal control, alongside live statistical analytics powered by a custom native graphing engine.

---

## 🖼️ Application Showcase

![SUMO Real-Time Traffic Simulation GUI](result_screenshot/Screenshot%202026-07-30%20200831.png)

*Figure 1: Full GUI Display showing the interactive map viewport (center), real-time simulation controls & vehicle injection panel (left), and live native statistical graph plotting (bottom-right).*

---

## 🎓 Academic Context & Team Roles

* **Course**: Java Programming
* **Institution**: Frankfurt University of Applied Sciences (Frankfurt UAS, Germany)
* **Semester**: Wintersemester 2025/2026

### 👥 Team Members & Contributions

| Member | Primary Role | Key Responsibilities |
| :--- | :--- | :--- |
| **Tran Khanh Nam** | **Team Lead** | - Developed the simulation wrapper architecture (`SimulationWrapper.java`)<br/>- Implemented the TraCI / `libsumo` protocol integration (`de.tudresden.sumo`)<br/>- Engineered background simulation stepping and thread synchronization |
| **Truong Thien Nhan** | **Simulation Controller** | - Implemented vehicle injection engine (single and batch injection)<br/>- Engineered vehicle parameter modification (speed, color, route filtering)<br/>- Implemented dynamic traffic light phase addition and phase control logic |
| **Nguyen Ho Nguyen** | **UI Engine & Analytics Developer** | - Built the SUMO network XML parsing engine (`Networkpaser.java`)<br/>- Developed high-performance 2D canvas rendering (`MapCanvas.java`)<br/>- Designed camera viewport math: **smooth panning (moving), scrolling zoom, auto-fit/centering** (`Transform.java`)<br/>- Created custom **real-time native graph plotting engine** (`Graph.java`) for live metric streams<br/>- Integrated UI controls, visual assets, and full GUI screenshot documentation |

---

## 🛠️ Technology Stack & Architecture

* **Core Language**: Java 17+
* **GUI Engine**: JavaFX 17 (FXML Layouts + 2D `Canvas` API for graphics rendering)
* **Simulation Engine**: [Eclipse SUMO](https://eclipse.dev/sumo/) (Simulation of Urban MObility)
* **API Interface**: TraCI (Traffic Control Interface via `TraaS` / `libsumo`)
* **Parsing Engine**: XML Parsing (`Networkpaser.java` for SUMO `.net.xml` nodes, edges, lanes, junctions)
* **Concurrency**: Multithreaded architecture (Dedicated `Sumo-Stepper` background thread paired with JavaFX `AnimationTimer` UI loop at ~60 FPS)
* **Analytics & Plotting**: Custom native JavaFX 2D graphing engine (`Graph.java`)
* **Reporting**: CSV dataset export and formatted PDF summary report generator (`Statistic.java`)

---

## 🚀 Core Features

### 1. 🗺️ Interactive Map Canvas & Viewport
- **Network Rendering**: Parses road networks, junctions, lanes, and active traffic light states directly from SUMO `.net.xml` files.
- **Viewport Navigation**: Smooth drag-to-pan camera movement, mouse-wheel scrolling zoom, and auto-fit to screen (`fitAndCenter`).
- **Real-Time Rendering**: Smooth ~60 FPS vehicle motion tracking with color-coded vehicle speeds and route highlights.

### 2. 🚦 Traffic Light Management & Vehicle Control
- **Manual Light Overrides**: Switch traffic light phases interactively and adjust phase durations on the fly.
- **Vehicle Injection**: Inject custom vehicles (single or batch) onto target edges with user-defined speed, color, and route assignments.
- **Filtering**: Live parameter-based filtering (filter visible vehicles by speed range or color).

### 3. 📊 Real-Time Native Statistics & Reporting
- **Native Live Graphing**: Custom 2D plotting component (`Graph.java`) rendering live metric streams (average speed, vehicle density per edge, congestion hotspots, travel time distribution) synchronized with simulation execution.
- **Data Export**: Export live simulation statistics to **CSV** format for offline analysis.
- **PDF Report Generation**: Generate automated PDF summaries featuring metric breakdowns, timestamps, and congested edge analysis.

---

## 📁 Repository Structure

```text
simulationrealtime/
├── result_screenshot/         # High-resolution application screenshots
│   └── Screenshot 2026-07-30 200831.png
├── SumoConfig/                # SUMO network configuration (.net.xml, .sumocfg)
├── lib/                       # External libraries (JavaFX SDK 17, TraaS.jar)
├── src/                       # Application source code
│   ├── gui/                   # User Interface & Graphics Engine
│   │   ├── MapCanvas.java     # 2D Network & Vehicle Renderer
│   │   ├── Transform.java     # Pan, Zoom & Viewport Matrix Transformations
│   │   ├── Graph.java         # Custom Native Real-Time Plotting Component
│   │   ├── ControlPanel.java  # FXML Controller & User Input Handlers
│   │   └── DecApp.fxml        # Main Dashboard Layout
│   ├── paser/                 # SUMO Network XML Parser
│   │   └── Networkpaser.java
│   ├── wrapper/               # SUMO TraCI Protocol Wrapper
│   │   ├── SimulationWrapper.java
│   │   └── DataType/          # Data Transfer Objects (VehicleData, TrafficLightData, etc.)
│   ├── tracker/               # Real-Time Statistics & Report Exporter (CSV/PDF)
│   │   └── Statistic.java
│   ├── logger/                # Logging Utility
│   └── App.java               # Application Main Entry Point
├── run.bat                    # Windows execution script
└── README.md                  # Project documentation
```

---

## 💻 Prerequisites & Setup Instructions

### System Requirements
* **Operating System**: Windows 10 / 11 (64-bit)
* **Java Development Kit**: JDK 17 or newer
* **Simulation Software**: [Eclipse SUMO](https://eclipse.dev/sumo/) (installed and configured on System `PATH`)

---

### 🚀 Running the Application

#### Option 1: Double-Click Executable (Recommended)
Navigate to the root directory and double-click `run.bat`.

#### Option 2: Windows Terminal / PowerShell
Open terminal in the project root directory and run:

```powershell
.\run.bat
```

#### Option 3: Manual Command Line Execution
```bash
cd src
javac --module-path "../lib/javafx-sdk-17.0.17/lib;." --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web -cp "../lib/TraaS.jar;../lib/JavaFX/javafx-swt.jar;." App.java
java --module-path "../lib/javafx-sdk-17.0.17/lib;." --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web -cp "../lib/TraaS.jar;../lib/JavaFX/javafx-swt.jar;." App
```

---

## 📜 License & Credits

Developed as part of the **Java Programming** coursework at **Frankfurt University of Applied Sciences** (Wintersemester 2025/2026). 
Special thanks to the open-source **Eclipse SUMO** team for providing the simulation engine.
