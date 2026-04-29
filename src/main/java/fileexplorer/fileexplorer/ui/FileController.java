package fileexplorer.fileexplorer.ui;

import fileexplorer.fileexplorer.model.Disk;
import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.DriverProvider;
import fileexplorer.fileexplorer.service.GoogleDriveManager;
import fileexplorer.fileexplorer.provider.LocalProvider;
import fileexplorer.fileexplorer.provider.StorageProvider;
import fileexplorer.fileexplorer.service.FileOperationService;
import fileexplorer.fileexplorer.ui.preview.PreviewFactory;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.Stack;

public class FileController {
    @FXML public ComboBox<Disk> rootsMenu;
    @FXML public TextArea currentPath;
    @FXML public Button goBack, copyButton, deleteButton, moveButton, renameButton, createFolder, logoutButton;
    @FXML public ListView<StorageItem> folderList;
    @FXML public ListView<StorageItem> fileList;
    private PreviewFactory previewFactory = new PreviewFactory();
    private final FileOperationService fileService = new FileOperationService();

    private StorageProvider provider;
    private String currentPathId = null;
    private Stack<String> pathStack = new  Stack<>();
    private GoogleDriveManager manager;

    @FXML
    private void initialize() {
        mainList();
        File[] roots = File.listRoots();
        for(File root : roots){
            rootsMenu.getItems().add(new Disk(root.getAbsolutePath(), root.getTotalSpace(), root.getFreeSpace()));
        }
        rootsMenu.getItems().add(new Disk("Google Drive", 0, 0));

        if(!rootsMenu.getItems().isEmpty()){
            rootsMenu.getSelectionModel().select(0);
            onSelected();
        }

        folderList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                fileList.getItems().setAll(newValue);
            }
            else{
                fileList.getItems().clear();
            }
        });

        fileList.setCellFactory(lv -> new javafx.scene.control.ListCell<StorageItem>(){
            @Override
            protected void updateItem(StorageItem item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null){
                    setGraphic(null);
                    setText(null);
                }
                else{
                    Node previewNode = previewFactory.getPreviewer(item, provider);
                    if(previewNode instanceof VBox){
                        VBox v = (VBox) previewNode;
                        v.prefWidthProperty().bind(lv.widthProperty().subtract(40));
                        v.prefHeightProperty().bind(lv.heightProperty().subtract(40));
                    }
                    setGraphic(previewNode);
                    setText(null);
                }

                setStyle("-fx-background-color: transparent;");
            }
        });
    }

    private void mainList(){
        folderList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        folderList.setCellFactory(lv -> new ListCell<>(){
            @Override
            protected void updateItem(StorageItem item, boolean empty){
                super.updateItem(item, empty);
                if(empty || item == null){
                    setText(null);
                }
                else{
                    String icon = item.isFolder() ? "\uD83D\uDCC1" : "\uD83D\uDCC4";
                    setText(icon+item.getName());
                }
            }
        });

        folderList.setOnMouseClicked(event -> {
            StorageItem selected = folderList.getSelectionModel().getSelectedItem();
            if(selected!=null && event.getClickCount() == 2 && selected.isFolder()){
                pathStack.push(currentPathId);
                currentPathId = selected.getId();
                currentPath.setText(currentPathId);
                loadFiles();
            }
        });
    }


    @FXML
    private void onSelected(){
        Disk selected = rootsMenu.getSelectionModel().getSelectedItem();
        if(selected == null){
            return;
        }

        pathStack.clear();
        boolean isDrive = "Google Drive".equals(selected.getPath());
        if(logoutButton != null){
            logoutButton.setVisible(isDrive);
            logoutButton.setManaged(isDrive);
        }
        if(isDrive){
            try{
                if(manager == null) manager = new GoogleDriveManager();
                String userNamePart = manager.getEmail();
                String finalName = "Google Drive (" + userNamePart + ")";
                provider = new DriverProvider(manager);
                currentPath.setText(finalName);
            }
            catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
        else{
            provider = new LocalProvider();
            currentPathId = selected.getPath();
            currentPath.setText(currentPathId);
        }
        loadFiles();
    }

    public void loadFiles(){
        Task<List<StorageItem>> task = new Task<>() {
            @Override
            protected List<StorageItem> call() throws Exception {
                return provider.listContents(currentPathId);
            }
        };

        task.setOnSucceeded(event -> {
            folderList.getItems().setAll(task.getValue());
            fileList.getItems().clear();
        });
        task.setOnFailed(event -> {
            System.out.println(task.getException().getMessage());
        });

        Thread thread = new  Thread(task);
        thread.start();
    }

    @FXML
    private void goBack(){
        if(pathStack.isEmpty()){
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You are on the roots.");
            alert.setTitle("Error");
            alert.setHeaderText("You are on the roots.");
            alert.showAndWait();
        }
        else{
            currentPathId = pathStack.pop();
            currentPath.setText(currentPathId);
            loadFiles();
            fileList.getItems().clear();
        }
    }

    @FXML
    private void handleLogOut(){
        manager.logout();
        Disk selected = rootsMenu.getSelectionModel().getSelectedItem();
        if(selected.toString().contains("Google Drive")){
            selected.setCostumeName("Google Drive");
        }
        rootsMenu.getSelectionModel().select(0);
        onSelected();
    }


    //Műveletek

    @FXML
    private void onDelete(){
        List<StorageItem> selected = getSelectedItems();
        if(selected.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,"Biztosan törölni szeretnéd az elemeket?");
        confirm.showAndWait().ifPresent(response -> {
            if(response == ButtonType.OK){
                Task<Void> task = fileService.deleteTask(selected, provider);

                task.setOnSucceeded(event -> {loadFiles();});
                task.setOnFailed(e-> new Alert(Alert.AlertType.INFORMATION,"Hiba"));

                new Thread(task).start();
            }
        });
    }

    @FXML
    private void onRename() {
        StorageItem selected = folderList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Átnevezés");
        dialog.setHeaderText("Aktuális név: " + selected.getName());
        dialog.setContentText("Kérlek, add meg az új nevet:");

        dialog.showAndWait().ifPresent(newName -> {
            String newNameTrim = newName.trim();
            if (newNameTrim.isEmpty() || newNameTrim.equals(selected.getName())) return;

            if (!selected.isFolder()) {
                String oldName = selected.getName();
                int lastDotOld = oldName.lastIndexOf('.');
                String orgExt = (lastDotOld != -1) ? oldName.substring(lastDotOld) : "";

                int lastDotNew = newNameTrim.lastIndexOf('.');

                if (lastDotNew == -1) {
                    newNameTrim += orgExt;
                } else {
                    String newExt = newNameTrim.substring(lastDotNew);

                    if (!newExt.equalsIgnoreCase(orgExt)) {
                        Alert warn = new Alert(Alert.AlertType.CONFIRMATION, "Biztosan meg szeretnéd változtatni a kiterjesztést?", ButtonType.YES, ButtonType.NO);
                        warn.setHeaderText("Kiterjesztés megváltoztatása.");
                        warn.showAndWait();

                        if (warn.getResult() == ButtonType.NO) {
                            newNameTrim = newNameTrim.substring(0, lastDotNew) + orgExt;
                        }
                    }
                }
            }


            Task<Void> task = fileService.renameTask(selected, provider, newName);

            task.setOnSucceeded(e -> {
                loadFiles();
            });

            task.setOnFailed(e -> {
                new Alert(Alert.AlertType.ERROR, "Hiba az átnevezés során: "
                        + task.getException().getMessage()).show();
            });

            new Thread(task).start();
        });
    }

    @FXML
    private void onCreate(){
        TextInputDialog dialog = new TextInputDialog("Új mappa");
        dialog.setTitle("Mappa létrehozása: ");
        dialog.setHeaderText("Új mappa helye: " + currentPathId);
        dialog.setContentText("Kérem adja meg a mappa nevét: ");

        dialog.showAndWait().ifPresent(newName -> {
            if(newName.isEmpty()){
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Kötelező megadni a mappa nevét: ");
                a.show();
            }

            try{
                provider.createFolder(currentPathId, newName);
                loadFiles();
            }catch(Exception e){
                Alert a = new Alert(Alert.AlertType.INFORMATION, e.getMessage());
                a.show();
            }
        });
    }


    // --- Getterek a MainControllernek
    public StorageProvider getProvider() {return  provider;}
    public String getCurrentPathId() {return  currentPathId;}
    public List<StorageItem> getSelectedItems() {return folderList.getSelectionModel().getSelectedItems();}
    public StorageItem getSelectedItem() {return fileList.getSelectionModel().getSelectedItem();}

}