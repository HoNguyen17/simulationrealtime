## 1. Objective

The objective of this project is to focus on applying several techniques to build a real-time traffic 
simulation. This project is about visualizing the traffic network, illustrating how entities within 
networks interact, and enabling users to control the traffic system. 

## 2. Program's Functionalities 
The program is currently supporting only window 11 users. Other window OS might work as well.
In general, the program's objective is to simulate the interaction of a simple traffic network, 
where users can manipulate the operations of the traffic components. Therefore, a few 
following core functionalities are following.
|               |                  |
|---------------|------------------|
|2.1 Map Visualization    |- Render road network<br/>- Display current traffic light phases<br/>- Show moving-colored vehicle<br/>- Support zooming, panning. |
|2.2 Vehicle Manipulation |- Allow injection of vehicles in a specific edge (both single and in batch).<br/>- Adjustment to vehicle parameters (consists of speed, color and route) by the user when injecting<br/>- Filter visible vehicles based on their parameter (this include color and speed). |
|2.3 Traffic Light Management |- Allow user to switch phase manually.<br/>- Make changes to a light phase duration. |
|2.4 Statistic Tracking and Exportable Report |
|2.4.1 Tracking |- Store record of metrics, including average speed, vehicle density per edge, congestion hotspots, travel time distribution.<br/>- Display charts and summaries in real time.|
|2.4.2 Exporting report |- Allow saving simulation statistics to CSV for external analysis.<br/>- Enable generating PDF summaries with charts, metrics, and timestamps with filtered congested edge|

## 3. Instructiona
### Step 1: Installation 
Download the whole git
Download Java SDK 17 or newer
Download Sumo
### Step 2: Run 
- Option 1: Run using window terminal

First open the terminal, then navigate to the `simulationrealtime` directory and ``run.bat``.
- Option 2: Run using powershell terminal

First open the terminal, then navigate to the `simulationrealtime` directory and ``.\run.bat``.
- Option 3: Click to run

Find the run.bat file and double click to start the program.


