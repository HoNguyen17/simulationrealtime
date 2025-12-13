package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import wrapper.SimulationWrapper; //to connect with wrapper

public class App_dec_controller {

    //button and container(pane)
    @FXML
    private StackPane mapContainer; // Thêm cái này để chứa MapCanvas

//    @FXML
//    private Slider sim_spd;

    @FXML
    private Button tfl_auto;

    @FXML
    private Button tfl_man;

    @FXML
    private Button tfl_nextp;

    @FXML
    private Button v_rview;

    @FXML
    private Button v_zin;

    @FXML
    private Button v_zout;


    //Add Map
    private MapCanvas mapCanvas; //to store map
    public SimulationWrapper simulationWrapper; //call function from wrapper,SUMO

    //Display map in GUI app
    public void setMapCanvas(MapCanvas mapCanvas) {
        this.mapCanvas = mapCanvas;

        //Bring GUI to dashboard, use for StackPane
        if (mapContainer != null) { //check if container(in fxml) existed?
            mapContainer.getChildren().add(mapCanvas.getCanvas()); //get components in mapContainer, then add mapCanvas into it
            mapCanvas.getCanvas().widthProperty().bind(mapContainer.widthProperty()); //bind canvas width with container; canvas as wide as container
            mapCanvas.getCanvas().heightProperty().bind(mapContainer.heightProperty()); //bind canvas height with container; canvas as high as container

//            mapCanvas.fitAndCenter(); //center map
//            mapCanvas.render(); //display map component; cars, edge, road markings
        }
    }
    //Set wrapper to apply on this controller
    public void setSimulationWrapper(SimulationWrapper wrapper) {
        this.simulationWrapper = wrapper;
    }


    // Function of buttons
    // Simulation speed control
//    @FXML
//    void initialize(){  //use for slider
//        sim_spd.valueProperty().addListener((obs, oldV, newV) -> {
//            System.out.println("Speed: " + newV.doubleValue());
//        });
//    }

//    @FXML
//    void listen_sim_spd(){
//        sim_spd.valueProperty().addListener((obs, oldV, newV) -> {
//            System.out.println("Speed: " + newV.doubleValue());
//        });
//    }

    // Traffic light control
    @FXML
    void onClick_tfl_auto() {
        System.out.println("TL: auto");
    }
    @FXML
    void onClick_tfl_man() {
        System.out.println("TL: manual");
    }
    @FXML
    void onClick_tfl_nextp() {
        System.out.println("TL: next phase");
    }

    // View control
    @FXML
    void onClick_v_zin() {
        if(mapCanvas != null) { //check mapCanvas, because if it's null, the app will crash
            mapCanvas.zoomAtCenter(1.1);
        }
    }
    @FXML
    void onClick_v_zout() {
        if(mapCanvas != null) {
            mapCanvas.zoomAtCenter(0.9);
        }
    }
    @FXML
    void onClick_v_rview() {
        if(mapCanvas != null) {
            mapCanvas.fitAndCenter();
            mapCanvas.render();
        }
    }

}
