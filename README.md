## 1. Goal

The objective of this project is to focus on applying several techniques to build a real-time traffic 
simulation. This project is about visualizing the traffic network, illustrating how entities within 
networks interact, and enabling users to control the traffic system. 

## 2. Objectives and Functionalities 

In general, the program's objective is to simulate the interaction of a simple traffic network, 
where users can manipulate the operations of the traffic components. Therefore, a few 
following core functionalities are following.
|               |                  |
|---------------|------------------|
|2.1 Map Visualization    |- Render road network<br/>- Display current traffic light phases<br/>- Show moving-colored vehicle<br/>- Support zooming, panning, and camera rotation. |
|2.2 Vehicle Manipulation |- Allow injection of vehicles in a specific edge (both single and in batch).<br/>- Adjustment to vehicle parameters (consists of speed, color and route) by the user<br/>- Filter visible vehicles based on their parameter (this include color, speed and position). |
|2.3 Traffic Light Management |- Allow user to switch phase manually.<br/>- Make changes to a light phase duration. |
|2.4 Statistic Tracking and Exportable Report |
|2.4.1 Tracking |- Store record of metrics, including average speed, vehicle density per edge, congestion hotspots, travel time distribution.<br/>- Display charts and summaries in real time.|
|2.4.2 Exporting report |- Allow saving simulation statistics to CSV for external analysis.<br/>- Enable generating PDF summaries with charts, metrics, and timestamps with filtered traffic object (cars, edges)|

## 3. Current State
