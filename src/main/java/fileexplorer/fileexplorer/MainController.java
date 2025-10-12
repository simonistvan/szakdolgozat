package fileexplorer.fileexplorer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MainController {

    @FXML private AnchorPane leftPanel;
    @FXML private AnchorPane rightPanel;

    private FileController leftController;
    private FileController rightController;

    @FXML
    public void initialize() throws IOException {
        FXMLLoader loaderL = new FXMLLoader(getClass().getResource("panels.fxml"));
        AnchorPane leftContent = loaderL.load();
        leftController = loaderL.getController();
        leftPanel.getChildren().add(leftContent);

        FXMLLoader loaderR = new FXMLLoader(getClass().getResource("panels.fxml"));
        AnchorPane rightContent = loaderR.load();
        rightController = loaderR.getController();
        rightPanel.getChildren().add(rightContent);

        rightController.folderList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        leftController.folderList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        leftController.copyButton.setOnAction(e ->
                {
                    try {
                        copyFunction(leftController.getSelectedFiles(), rightController.currentDir);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
        );

        rightController.copyButton.setOnAction(e -> {
            try{
                copyFunction(rightController.getSelectedFiles(), leftController.currentDir);
            }
            catch (IOException ex){
                throw new RuntimeException(ex);
            }
        });

        rightController.deleteButton.setOnAction(e -> {
            try{
                deleteFunction(rightController.getSelectedFiles());
            }
            catch (IOException ex){
                throw new RuntimeException(ex);
            }
        });

        leftController.deleteButton.setOnAction(e -> {
            try{
                deleteFunction(leftController.getSelectedFiles());
            }
            catch (IOException ex){
                throw new RuntimeException(ex);
            }
        });

        leftController.renameButton.setOnAction(e -> {
            File f = leftController.folderList.getSelectionModel().getSelectedItem();
            try{
                renameFunction(f);
            }
            catch(IOException ex){
                throw new RuntimeException(ex);
            }
        });

        rightController.renameButton.setOnAction(e -> {
            File f = rightController.folderList.getSelectionModel().getSelectedItem();
            try{
                renameFunction(f);
            }
            catch (IOException ex){
                throw new RuntimeException(ex);
            }
        });

        rightController.moveButton.setOnAction(e -> {
            try{
                moveFunction(rightController.getSelectedFiles(), leftController.currentDir);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        leftController.moveButton.setOnAction(e -> {
            try{
                moveFunction(leftController.getSelectedFiles(), rightController.currentDir);
            }
            catch (IOException ex){
                throw new RuntimeException(ex);
            }
        });
    }

    public void moveFunction(List<File> files, File destDir) throws IOException {
        for(File f : files){
            copyRecursive(f, destDir);
            deleteRecursive(f);
        }
    }

    public void renameFunction(File f) throws IOException {
        if(f.isDirectory()){
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Rename File");
            dialog.setHeaderText("Rename File");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try{
                    File newDir = new File(f.getParentFile(), result.get().trim());
                    java.nio.file.Files.copy(f.toPath(), newDir.toPath());
                    File[] files = f.listFiles();
                    for(File fi : files ){
                        copyRecursive(fi, newDir);
                    }
                    deleteRecursive(f);
                }
                catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        }
        else{
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Rename File");
            dialog.setHeaderText("Rename File");
            Optional<String> result = dialog.showAndWait();

            String extension = "";
            int index = f.getName().lastIndexOf('.');
            if(index != -1){
                extension = f.getName().substring(index);
            }
            if (result.isPresent()) {
                String name = result.get().trim();
                try{
                    File newDir = new File(f.getParent(), name+extension);
                    java.nio.file.Files.move(f.toPath(), newDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        }

        rightController.loadFolders(rightController.currentDir);
        leftController.loadFolders(leftController.currentDir);
    }

    public void deleteFunction(List<File> files) throws IOException {
        for(File f : files){
            deleteRecursive(f);
        }
    }

    public void copyFunction(List<File> src, File destDir) throws IOException {
        for(File f : src){
            copyRecursive(f, destDir);
        }
    }

    private void deleteRecursive(File f) throws IOException {
        if(f.isDirectory() && Objects.requireNonNull(f.listFiles()).length > 0){
            File[] src = f.listFiles();
            for(File c : src){
                deleteRecursive(c);
            }
            deleteRecursive(f);
        }
        else if(f.isDirectory() && Objects.requireNonNull(f.listFiles()).length == 0){
            try {
                java.nio.file.Files.delete(f.toPath());
            }
            catch (NoSuchFileException ex){
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("File not found");
                alert.showAndWait();
            }
        }
        else{
            try{
                java.nio.file.Files.delete(f.toPath());
            }
            catch(Exception ex){
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("File not found");
                alert.showAndWait();
            }
        }
        rightController.loadFolders(rightController.currentDir);
        leftController.loadFolders(leftController.currentDir);

    }
   private void copyRecursive(File file, File destDir) throws IOException {
       File newDir = new File(destDir, file.getName());

       if (file.isDirectory()) {
           if (newDir.exists()) {
               Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
               alert.setTitle("Overwrite");
               alert.setHeaderText(null);
               alert.setContentText("The folder '" + newDir.getName() + "' already exists. Overwrite?");

               Optional<ButtonType> result = alert.showAndWait();
               if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
                   return;
               }
           }

           newDir.mkdirs();

           File[] files = file.listFiles();
           if (files != null) {
               for (File f : files) {
                   copyRecursive(f, newDir);
               }
           }

       } else {
           try {
               java.nio.file.Files.copy(
                       file.toPath(),
                       newDir.toPath(),
                       StandardCopyOption.REPLACE_EXISTING
               );
           } catch (IOException e) {
               Alert alertError = new Alert(Alert.AlertType.INFORMATION);
               alertError.setTitle("Error");
               alertError.setHeaderText(null);
               alertError.setContentText("Error during copying the file: " + file.getName());
               alertError.showAndWait();
           }
       }

       rightController.loadFolders(rightController.currentDir);
       leftController.loadFolders(leftController.currentDir);
   }

}
