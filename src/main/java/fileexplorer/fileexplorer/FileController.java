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
    @FXML Button copyButton;
    @FXML Button deleteButton;
    @FXML Button renameButton;
    @FXML Button moveButton;
    @FXML ListView<File> folderList;
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
        if(currentDir.getParent() != null){
            currentDir = currentDir.getParentFile();
            loadFolders(currentDir);
            currentPath.setText(currentDir.getPath());
            fileList.getItems().clear();
        }
        else{
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("You can't change directory from the root.");
            alert.show();
        }
    }



   @FXML
   private void initialize(){
        File[] roots = File.listRoots();
        for(File root : roots){
            rootsMenu.getItems().add(new Disk(root.getPath(), root.getTotalSpace(), root.getFreeSpace()));
        }

        rootsMenu.getSelectionModel().selectFirst();
        currentDir = new File(rootsMenu.getSelectionModel().getSelectedItem().getPath());
        currentPath.setText(currentDir.getPath());
        loadFolders(currentDir);

       folderList.setOnMouseClicked((MouseEvent event) -> {
           File selectedFolder = folderList.getSelectionModel().getSelectedItem();
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
                   if(files == null) return;
                   List<File> foldersSort = new  ArrayList<>();
                   List<File> filesSort = new ArrayList<>();

                   for (File file : files) {
                       if(file.isDirectory()){
                           foldersSort.add(file);
                       }
                       else{
                           filesSort.add(file);
                       }
                   }

                   foldersSort.sort((a,b)->a.getName().compareToIgnoreCase(b.getName()));
                   filesSort.sort((a,b)->a.getName().compareToIgnoreCase(b.getName()));

                   List<File> allFiles = new   ArrayList<>();
                   allFiles.addAll(foldersSort);
                   allFiles.addAll(filesSort);
                   fileList.getItems().addAll(allFiles);
               }
           }
       });

       folderList.setCellFactory(id -> new ListCell<File>() {
           protected void updateItem(File file, boolean empty) {
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
            if(files!= null){
                List<File> foldersSort = new  ArrayList<>();
                List<File> filesSort = new ArrayList<>();

                for (File file : files) {
                    if(file.isDirectory()){
                        foldersSort.add(file);
                    }
                    else{
                        filesSort.add(file);
                    }
                }

                foldersSort.sort((a,b)->a.getName().compareToIgnoreCase(b.getName()));
                filesSort.sort((a,b)->a.getName().compareToIgnoreCase(b.getName()));

                List<File> allFiles = new   ArrayList<>();
                allFiles.addAll(foldersSort);
                allFiles.addAll(filesSort);

                folderList.getItems().addAll(allFiles);
            }
        }
    }

    public List<File> getSelectedFiles(){
        List<File> files = new ArrayList<>(folderList.getSelectionModel().getSelectedItems());
        List<File> filesreturn = new ArrayList<>();
        for(File f : files){
            filesreturn.add(new File(f.getPath()));
        }
        return filesreturn;
    }
}
