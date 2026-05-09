package fileexplorer.fileexplorer.ui;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.service.FileOperationService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private AnchorPane leftPanel;
    @FXML private AnchorPane rightPanel;

    private FileController leftController;
    private FileController rightController;
    private final FileOperationService fileService = new FileOperationService();

    @FXML
    public void initialize() throws IOException {
        leftController = loadPanel(leftPanel);
        rightController = loadPanel(rightPanel);

        setupActions();
    }

    private FileController loadPanel(AnchorPane container) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fileexplorer/fileexplorer/panels.fxml"));
        VBox content = loader.load();

        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        container.getChildren().add(content);
        return loader.getController();
    }

    private void setupActions() {
        // Balról jobbra műveletek
        leftController.copyButton.setOnAction(e -> executeOperation(leftController, rightController, "copy"));
        leftController.moveButton.setOnAction(e -> executeOperation(leftController, rightController, "move"));

        // Jobbról balra műveletek
        rightController.copyButton.setOnAction(e -> executeOperation(rightController, leftController, "copy"));
        rightController.moveButton.setOnAction(e -> executeOperation(rightController, leftController, "move"));
    }

    private void executeOperation(FileController src, FileController dest, String type) {
        List<StorageItem> selected = src.getSelectedItems();
        if (selected.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Nincs kijelölt elem!").show();
            return;
        }
        String destPath = dest.getCurrentPathId();

        String finalDestPath = (destPath == null || destPath.isEmpty() || "Google Drive".equalsIgnoreCase(destPath))
                ? "root"
                : destPath;

        Task<Void> task;
        String actionName = "move".equalsIgnoreCase(type) ? "Áthelyezés" : "Másolás";

        if ("move".equalsIgnoreCase(type)) {
            task = fileService.moveTask(selected, src.getProvider(), dest.getProvider(), finalDestPath);
        } else {
            task = fileService.copyTask(selected, src.getProvider(), dest.getProvider(), finalDestPath);
        }

        Alert progressDialog = new Alert(Alert.AlertType.INFORMATION);
        progressDialog.setTitle("Folyamatban...");
        progressDialog.setHeaderText(actionName + " folyamatban...");

        ProgressBar pb = new ProgressBar();
        pb.setPrefWidth(300);
        pb.progressProperty().bind(task.progressProperty());
        progressDialog.getDialogPane().setContent(pb);

        progressDialog.getButtonTypes().clear();

        task.setOnSucceeded(ev -> {
            progressDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            progressDialog.close();
            dest.loadFiles();
            if ("move".equalsIgnoreCase(type)) src.loadFiles();
            new Alert(Alert.AlertType.INFORMATION, "Sikeresen befejeződött!").show();
        });

        task.setOnFailed(ev -> {
            progressDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            progressDialog.close();
            new Alert(Alert.AlertType.ERROR, "Hiba: " + task.getException().getMessage()).show();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        progressDialog.showAndWait();


    }
}