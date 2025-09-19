
package fileexplorer.fileexplorer;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

public class MainController {
    @FXML
    private AnchorPane leftPanel;
    @FXML
    private AnchorPane rightPanel;

    private FileController leftController;
    private FileController rightController;

    private void initialize() {
        leftController = (FileController) leftPanel.getProperties().get("fx:controller");
        rightController = (FileController) rightPanel.getProperties().get("fx:controller");
    }
}