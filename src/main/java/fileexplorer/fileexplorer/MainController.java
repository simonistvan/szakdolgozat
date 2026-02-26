package fileexplorer.fileexplorer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
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

        rightController.copyButton.setOnAction(e -> {
            try {
                copyFunction(
                        rightController,
                        rightController.getSelectedFiles(),
                        leftController.currentDir,
                        leftController
                );
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        leftController.copyButton.setOnAction(e -> {
            try {
                copyFunction(
                        leftController,
                        leftController.getSelectedFiles(),
                        rightController.currentDir,
                        rightController
                );
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        rightController.deleteButton.setOnAction(e -> {
            try {
                deleteFunction(rightController ,rightController.getSelectedFiles());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        leftController.deleteButton.setOnAction(e -> {
            try {
                deleteFunction(leftController ,leftController.getSelectedFiles());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        leftController.renameButton.setOnAction(e -> {
            VirtualFile vf = leftController.folderList.getSelectionModel().getSelectedItem();
            if (vf != null) {
                try {
                    renameFunction(vf, leftController);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        rightController.renameButton.setOnAction(e -> {
            VirtualFile vf = rightController.folderList.getSelectionModel().getSelectedItem();
            if (vf != null) {
                try {
                    renameFunction(vf, rightController);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        leftController.moveButton.setOnAction(e -> {
            try {
                moveFunction(leftController.getSelectedFiles(), rightController.currentDir);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        rightController.moveButton.setOnAction(e -> {
            try {
                moveFunction(rightController.getSelectedFiles(), leftController.currentDir);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        leftController.createFolder.setOnAction(e -> leftController.createFolder());
        rightController.createFolder.setOnAction(e -> rightController.createFolder());
    }

    public void moveFunction(List<VirtualFile> files, File destDir) throws IOException {
        for(VirtualFile vf : files){
            if(!vf.isDrive()) {
                File f = new File(vf.getPathOrId());
                copyRecursive(f, destDir);
                deleteRecursive(f);
            }
        }
    }

    public void renameFunction(VirtualFile vf, FileController controller) throws IOException {
        TextInputDialog name = new TextInputDialog(vf.getName());
        name.setTitle("Rename");
        name.setContentText("Enter the new file name");
        name.setHeaderText("File Rename");

        Optional<String> result = name.showAndWait();

        if(result.isPresent() && !result.get().trim().isEmpty()){
            String newName = result.get().trim();
            try{
                if(vf.isDrive()){
                    controller.renameDriveFile(vf.getPathOrId(), newName);
                }
                else{
                    File oldFile = new File(vf.getPathOrId());
                    Path source = oldFile.toPath();

                    //Kiterjesztés megőrzése
                    String extension = "";
                    if (!oldFile.isDirectory()) {
                        int dotIndex = oldFile.getName().lastIndexOf('.');
                        if (dotIndex != -1) extension = oldFile.getName().substring(dotIndex);
                    }

                    String finalName = newName.contains(".") ? newName : newName + extension;
                    Files.move(source, source.resolveSibling(finalName), StandardCopyOption.REPLACE_EXISTING);
                }
                controller.refreshView();
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Can't rename the file");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        }

    }

    public void deleteFunction(FileController srcController, List<VirtualFile> files) throws IOException {
        if(files == null || files.isEmpty()) return;

        boolean localDeleted = false;

        for(VirtualFile vf : files){
            if(!vf.isDrive()) {
                File f = new File(vf.getPathOrId());
                if (f.exists()) {
                    deleteRecursive(f); // Meghívja a javított rekurziót
                    localDeleted = true;
                }
            }
        }

        // Lokális frissítés: CSAK EGYSZER a folyamat végén
        if (localDeleted) {
            leftController.loadFolders(leftController.currentDir);
            rightController.loadFolders(rightController.currentDir);
        }

        List<VirtualFile> driver = files.stream()
                .filter(VirtualFile::isDrive)
                .toList();

        if(!driver.isEmpty()){
            srcController.deleteDriveSmart(driver, srcController::loadGoogleDriveFiles);
        }
    }

    public void copyFunction(
            FileController srcController,
            List<VirtualFile> src,
            File destDir,
            FileController destController) throws IOException {

        for (VirtualFile vf : src) {
            if (!vf.isDrive()) {
                copyRecursive(new File(vf.getPathOrId()), destDir);
            }
        }

        List<VirtualFile> driver = src.stream()
                .filter(VirtualFile::isDrive)
                .toList();

        if (!driver.isEmpty()) {
            srcController.copyDriverToLocal(
                    driver,
                    destDir,
                    () -> destController.loadFolders(destDir)  // 🔥 CÉL PANEL
            );
        }
    }

    private void deleteRecursive(File f) throws IOException {
        if (f.isDirectory()) {
            File[] src = f.listFiles();
            if (src != null) {
                for (File c : src) {
                    deleteRecursive(c);
                }
            }
        }

        try {
            java.nio.file.Files.delete(f.toPath());
        } catch (NoSuchFileException e){}
    }

    private void copyRecursive(File file, File destDir) throws IOException {
        File newFileOrDir = new File(destDir, file.getName());

        if (file.isDirectory()) {
            if (!newFileOrDir.exists()) {
                newFileOrDir.mkdirs();
            }
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    copyRecursive(f, newFileOrDir);
                }
            }
        } else {
            java.nio.file.Files.copy(
                    file.toPath(),
                    newFileOrDir.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

}
