package fileexplorer.fileexplorer;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javafx.scene.input.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileController{
    File currentDir;

    @FXML ComboBox<Disk> rootsMenu;
    @FXML TextArea currentPath;
    @FXML Button goBack;
    @FXML ListView<Folder> folderList;
    @FXML ListView<File> fileList;

    @FXML
    private void onSelected(){ // ezt hívom meg a meghajtó választáskor.
        folderList.getItems().clear();
        fileList.getItems().clear();
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

       folderList.setCellFactory(id -> new ListCell<Folder>() {
           protected void updateItem(Folder file, boolean empty) {
               super.updateItem(file, empty);
               if (empty || file == null) {
                   setText(null);
               }
               else {
                   FileTime fileTime;
                   File newfolder = new File(file.getPath());
                   try {
                       fileTime = Files.getLastModifiedTime(Path.of(newfolder.getPath()));
                   } catch (IOException e) {
                       throw new RuntimeException(e);
                   }
                   String clock = fileTime.toString().substring(11, 16);
                   if(newfolder.isDirectory()){
                       setText("\uD83D\uDCC1\t" + newfolder.getName() + "\t" +fileTime.toString().substring(0, 10)+" "+ clock);
                   }
                   else if(!newfolder.isDirectory()){
                       setText("\uD83D\uDDCE\t" + newfolder.getName() + "\t" +fileTime.toString().substring(0, 10)+" "+clock);
                   }
               }
           }
       });

       fileList.setCellFactory(id -> new ListCell<File>(){
           protected void updateItem(File file, boolean empty){
               super.updateItem(file, empty);
               if(empty || file == null){
                   setText(null);
               }
               else{
                   FileTime fileTime;
                   if(file.isDirectory()){
                       try {
                           fileTime = Files.getLastModifiedTime(file.toPath());
                       } catch (IOException e) {
                           throw new RuntimeException(e);
                       }
                       String clock = fileTime.toString().substring(11, 16);
                       setText("\uD83D\uDCC1\t"+file.getName()+ "\t" + fileTime.toString().substring(0, 10) + " " + clock);
                   }
                   else if(!file.isDirectory()){
                       try{
                           fileTime = Files.getLastModifiedTime(Path.of(file.getPath()));
                       } catch (IOException e) {
                           throw new RuntimeException(e);
                       }
                       String clock = fileTime.toString().substring(11, 16);
                       setText("\uD83D\uDDCE\t"+file.getName()+ "\t" + fileTime.toString().substring(0, 10) + " " + clock);
                   }
               }
           }
       });

       ObservableList<File> files = FXCollections.observableArrayList();

       FXCollections.sort(files, (f1, f2) -> {
           if (f1.isDirectory() && !f2.isDirectory()) {
               return -1;
           } else if (!f1.isDirectory() && f2.isDirectory()) {
               return 1;
           } else {
               return f1.getName().compareToIgnoreCase(f2.getName());
           }
       });

    }


    @FXML
    public void loadFolders(File currentDir) {
        folderList.getItems().clear();
        fileList.getItems().clear();

        if(currentDir != null && !currentDir.getPath().isEmpty()){
            File[] files = currentDir.listFiles();
            if(files != null){
                    ObservableList<File> file_sort = FXCollections.observableArrayList();
                ObservableList<Folder> folder_sort = FXCollections.observableArrayList();
                for(File f : files){
                    if(f.isDirectory()){
                        folder_sort.add(new Folder(f.getPath(), f.getName()));
                    }
                    else if(f.isFile()){
                        file_sort.add(f);
                    }
                }

                FXCollections.sort(file_sort, (a,b)->a.getName().compareToIgnoreCase(b.getName()));
                FXCollections.sort(folder_sort, (a,b)->a.getName().compareToIgnoreCase(b.getName()));

                folderList.getItems().addAll(folder_sort);
                fileList.getItems().addAll(file_sort);

            }
        }
    }
}
