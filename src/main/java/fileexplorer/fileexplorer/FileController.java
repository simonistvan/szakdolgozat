package fileexplorer.fileexplorer;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import javafx.scene.input.MouseEvent;
import java.io.File;

public class FileController{
    File currentDir;

    @FXML ComboBox<Disk> rootsMenu;
    @FXML TextArea currentPath;
    @FXML Button goBack;
    @FXML ListView<Folder> folderList;
    @FXML ListView<File> fileList;

    @FXML
    private void onSelected(){
        Disk selected = rootsMenu.getSelectionModel().getSelectedItem();
        if(selected != null && !selected.getPath().isEmpty()){
            currentDir = new File(rootsMenu.getSelectionModel().getSelectedItem().getPath());
            currentPath.setText(selected.getPath());
            folderList.getItems().clear();
            loadFolders(currentDir);
        }
    }

    @FXML //initialize-ban van megírva a logikája
    private void selectFolder(MouseEvent event){

    }

    @FXML
    private void goBack(MouseEvent event){
        currentDir = currentDir.getParentFile();
        loadFolders(currentDir);
        currentPath.setText(currentDir.getPath());
        fileList.getItems().clear();
    }



   @FXML
   private void initialize(){
        File[] roots = File.listRoots();
        for(File root : roots){
            rootsMenu.getItems().add(new Disk(root.getPath(), root.getTotalSpace(), root.getFreeSpace()));
        }

        rootsMenu.getSelectionModel().selectFirst();
        currentDir = new File(rootsMenu.getSelectionModel().getSelectedItem().toString());
        currentPath.setText(currentDir.getPath());
        loadFolders(currentDir);

       folderList.setOnMouseClicked((MouseEvent event) -> {
           Folder selectedFolder = folderList.getSelectionModel().getSelectedItem();
           if (selectedFolder == null) return;

           File selectedFile = new File(selectedFolder.getPath());

           if (event.getClickCount() == 2) {
               if (selectedFile.isDirectory()) {
                   fileList.getItems().clear();
                   currentDir = new File(selectedFile.getPath());
                   loadFolders(currentDir);
                   currentPath.setText(currentDir.getPath());
               }
           } else if (event.getClickCount() == 1) {
               if (selectedFile.isDirectory()) {
                   fileList.getItems().clear();
                   File[] files = selectedFile.listFiles();
                   if (files != null) {
                       for (File f : files) {
                            fileList.getItems().add(f);
                       }
                   }
               }
           }
       });

    }


    @FXML
    public void loadFolders(File currentDir){
        folderList.getItems().clear();
        if(currentDir.isDirectory()){
            File[] files = currentDir.listFiles();
            if(files!=null){
                for(File file : files){
                    folderList.getItems().add(new Folder(file.getPath(), file.getName()));
                }
            }
        }
    }
}
