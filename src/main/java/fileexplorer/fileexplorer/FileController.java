package fileexplorer.fileexplorer;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileController {
    private static final Log log = LogFactory.getLog(FileController.class);
    File currentDir;

    @FXML ComboBox<Disk> rootsMenu;
    @FXML TextArea currentPath;
    @FXML Button goBack;
    @FXML Button copyButton;
    @FXML Button deleteButton;
    @FXML Button renameButton;
    @FXML Button moveButton;
    @FXML ListView<VirtualFile> folderList;
    @FXML ListView<VirtualFile> fileList;
    @FXML Button downoloadButton;
    @FXML Button createFolder;

    private GoogleDriveManager driveManager;
    @FXML private ListView<com.google.api.services.drive.model.File> driveFiles;
    private String currentFolderId = null;
    private Stack<String> driverFolderStack = new Stack<>();

    @FXML
    private void onSelected() throws IOException, GeneralSecurityException {

        // ezt hívom meg a meghajtó választáskor.
        folderList.getItems().clear();
        fileList.getItems().clear();
        Disk selected = rootsMenu.getSelectionModel().getSelectedItem();
        if(selected.getPath().toString().equals("Google Drive")){
            TempForDrive tempDrive = new TempForDrive();
            currentDir = tempDrive.getTempRoot();
            currentPath.setText("Google Drive");

            if(driveManager == null){
                driveManager = new GoogleDriveManager();
            }

            currentFolderId = null;
            driverFolderStack.clear();
            loadGoogleDriveFiles();  // csak listáz
            downoloadButton.setVisible(false); // ha ez letöltéshez van

        }
        else if(selected != null && !selected.getPath().isEmpty()){
            currentDir = new File(selected.getPath());
            currentPath.setText(selected.getPath());
            folderList.getItems().clear();
            loadFolders(currentDir);
            downoloadButton.setVisible(false);
        }
    }

    @FXML
    private void selectFolder(MouseEvent event){ }

    @FXML
    public void loadGoogleDriveFiles() {
        try {
            if (driveManager == null) {
                driveManager = new GoogleDriveManager();
            }

            List<com.google.api.services.drive.model.File> googleFiles =
                    currentFolderId == null
                            ? driveManager.listFilesInRoot(100)
                            : driveManager.listFilesInFolder(currentFolderId, 100);

            folderList.getItems().clear();

            List<VirtualFile> newList = new ArrayList<>();
            for (com.google.api.services.drive.model.File file : googleFiles) {
                boolean isFolder = "application/vnd.google-apps.folder".equals(file.getMimeType());
                newList.add(new VirtualFile(file.getId(), file.getName(), isFolder));
            }

            folderList.getItems().addAll(newList);

        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void goBack(MouseEvent event){
        if(isDriveMode()){
            if(!driverFolderStack.isEmpty()) {
                currentFolderId = driverFolderStack.pop();

                loadGoogleDriveFiles();

                if (currentFolderId == null) {
                    currentPath.setText("Google Drive");
                } else {
                    currentPath.setText("Google Drive / " + currentFolderId);

                }
            }
            else{
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Info");
                alert.setHeaderText(null);
                alert.setContentText("You're already in the root");
                alert.showAndWait();
            }
        }
        else{
            if(currentDir!=null && currentDir.getParent() != null) {
                currentDir = currentDir.getParentFile();
                loadFolders(currentDir);
                currentPath.setText(currentDir.getPath());
                fileList.getItems().clear();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("You can't change directory from the root.");
                alert.show();
            }
        }
    }

    @FXML
    private void initialize(){
        downoloadButton.setVisible(false);
        File[] roots = File.listRoots();
        for(File root : roots){
            rootsMenu.getItems().add(new Disk(root.getPath(), root.getTotalSpace(), root.getFreeSpace()));
        }
        rootsMenu.getItems().add(new Disk("Google Drive", 0, 0));

        rootsMenu.getSelectionModel().selectFirst();
        currentDir = new File(rootsMenu.getSelectionModel().getSelectedItem().getPath());
        currentPath.setText(currentDir.getPath());
        loadFolders(currentDir);

        folderList.setOnMouseClicked((MouseEvent event) -> {
            VirtualFile selectedFolder = folderList.getSelectionModel().getSelectedItem();
            if (selectedFolder == null) return;

            if (event.getClickCount() == 2 && selectedFolder.isFolder()) {
                if (selectedFolder.isDrive()) {
                    driverFolderStack.push(currentFolderId);

                    currentFolderId = selectedFolder.getPathOrId();
                    loadGoogleDriveFiles();

                    currentPath.setText("Google Drive / " + selectedFolder.getName());
                } else {
                    currentDir = new File(selectedFolder.getPathOrId());
                    loadFolders(currentDir);
                    currentPath.setText(currentDir.getPath());
                }
            } /*else if (event.getClickCount() == 1 && selectedFolder.isFolder()) {
                listFiles(selectedFolder);
            }*/
        });
        folderList.setCellFactory(id -> new ListCell<>() {
            protected void updateItem(VirtualFile file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                }
                else {
                    if (!file.isDrive()) {
                        try {
                            FileTime fileTime = Files.getLastModifiedTime(Path.of(file.getPathOrId()));
                            String clock = fileTime.toString().substring(11, 16);
                            setText((file.isFolder() ? "\uD83D\uDCC1\t" : "\uD83D\uDDCE\t") +
                                    file.getName() + "\t" + fileTime.toString().substring(0, 10) + " " + clock);
                        } catch (IOException e) {
                            setText(file.getName());
                        }
                    } else {
                        // Google Drive fájloknál nincs helyi timestamp
                        setText((file.isFolder() ? "\uD83D\uDCC1\t" : "\uD83D\uDDCE\t") + file.getName());
                    }
                }
            }
        });
    }

    @FXML
    public void loadFolders(File currentDir) {
        folderList.getItems().clear();
        fileList.getItems().clear();

        if(currentDir != null && !currentDir.getPath().isEmpty()){
            File[] filesArray = currentDir.listFiles();
            if(filesArray!= null){
                List<VirtualFile> foldersSort = new ArrayList<>();
                List<VirtualFile> filesSort = new ArrayList<>();

                for (File f : filesArray) {
                    VirtualFile vf = new VirtualFile(f);
                    if(vf.isFolder()){
                        foldersSort.add(vf);
                    }
                    else{
                        filesSort.add(vf);
                    }
                }

                foldersSort.sort((a,b)->a.getName().compareToIgnoreCase(b.getName()));
                filesSort.sort((a,b)->a.getName().compareToIgnoreCase(b.getName()));

                List<VirtualFile> allFiles = new ArrayList<>();
                allFiles.addAll(foldersSort);
                allFiles.addAll(filesSort);

                folderList.getItems().addAll(allFiles);
            }
        }
    }

    public void listFiles(VirtualFile selectedPath) {
        fileList.getItems().clear();

        if (selectedPath == null) {
            return;
        }

        if (selectedPath.isFolder()) {
            if (selectedPath.isDrive()) {
                log.info("Drive mappa kijelölve, de a navigációhoz dupla kattintás szükséges.");
            }
            else {
                // LOKÁLIS ESET
                File save = currentDir;
                List<VirtualFile> filesSort = new ArrayList<>();
                List<VirtualFile> foldersSort = new ArrayList<>();
                currentDir = new File(selectedPath.getPathOrId());
                File[] files = currentDir.listFiles();

                if (files == null) {
                    System.err.println("Nem tudom listázni a fájlokat: " + currentDir.getAbsolutePath());
                    currentDir = save;
                    return;
                }

                for (File f : files) {
                    VirtualFile vf = new VirtualFile(f);
                    if (vf.isFolder()) {
                        foldersSort.add(vf);
                    } else {
                        filesSort.add(vf);
                    }
                }

                foldersSort.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                filesSort.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                List<VirtualFile> allFiles = new ArrayList<>(foldersSort);
                allFiles.addAll(filesSort);

                fileList.getItems().addAll(allFiles);
                currentDir = save;
            }
        }
    }

    public List<VirtualFile> getSelectedFiles(){
        List<VirtualFile> selected = new ArrayList<>(folderList.getSelectionModel().getSelectedItems());
        List<VirtualFile> result = new ArrayList<>();
        for(VirtualFile f : selected){
            result.add(f);
        }
        return result;
    }


    public void copyDriverToLocal(List<VirtualFile> driver, File destDir, Runnable onDone) {

        final List<VirtualFile> items = (driver == null) ? List.of() : List.copyOf(driver);
        final File targetDir = destDir;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                if (driveManager == null) driveManager = new GoogleDriveManager();

                int total = items.size();
                int i = 0;

                for (VirtualFile f : items) {
                    if (!f.isDrive()) continue;
                    if (f.isFolder()) continue;

                    i++;
                    updateMessage("Letöltés: " + f.getName() + " (" + i + "/" + total + ")");

                    File out = new File(targetDir, f.getName());
                    driveManager.downloadFile(f.getPathOrId(), out);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (onDone != null) {
                onDone.run();
            }
            new Alert(Alert.AlertType.INFORMATION, "Letöltés kész ✅").show();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace(); // Hiba kiírása
            ex.printStackTrace(); // Hiba kiírásadeke
            new Alert(Alert.AlertType.ERROR,
                    "Hiba letöltés közben:\n" + (ex != null ? ex.getMessage() : "ismeretlen")).show();
        });

        new Thread(task, "drive-download").start();
    }



    public void deleteDriveSmart(List<VirtualFile> driveItems, Runnable onDone) {
        if(driveItems == null || driveItems.isEmpty()) return;

        final List<VirtualFile> items = List.copyOf(driveItems);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if(driveManager == null) driveManager = new GoogleDriveManager();
                Drive drive = driveManager.getDriveService();

                final String email = drive.about().get()
                        .setFields("user(emailAddress)")
                        .execute()
                        .getUser()
                        .getEmailAddress();

                int deleted = 0;
                int failed = 0;
                List<String> failedNames = new ArrayList<>();

                for(VirtualFile f : items){
                    if(!f.isDrive()) continue;

                    try{
                        String fileId = f.getPathOrId();

                        var meta = drive.files()
                                .get(fileId)
                                .setSupportsAllDrives(true)
                                .setFields("id,name,ownedByMe")
                                .execute();

                        if(Boolean.TRUE.equals(meta.getOwnedByMe())){
                            // saját fájl -> kuka
                            var patch = new com.google.api.services.drive.model.File();
                            patch.setTrashed(true);

                            drive.files()
                                    .update(fileId, patch)
                                    .setSupportsAllDrives(true)
                                    .execute();

                            deleted++;
                        } else {
                            var plist = drive.permissions()
                                    .list(fileId)
                                    .setSupportsAllDrives(true)
                                    .setFields("permissions(id,emailAddress,type)")
                                    .execute();

                            if(plist.getPermissions() == null){
                                failed++;
                                failedNames.add(meta.getName() + " – nincs jogosultság");
                                continue;
                            }

                            Permission mine = plist.getPermissions().stream()
                                    .filter(p -> "user".equals(p.getType()))
                                    .filter(p -> email.equalsIgnoreCase(p.getEmailAddress()))
                                    .findFirst()
                                    .orElse(null);

                            if(mine == null){
                                failed++;
                                failedNames.add(meta.getName() + " – Nincs törlési jogosultságod!");
                                continue;
                            }

                            drive.permissions()
                                    .delete(fileId, mine.getId())
                                    .setSupportsAllDrives(true)
                                    .execute();

                            deleted++;
                        }

                    } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException ex){
                        failed++;
                        if(ex.getStatusCode() == 403) failedNames.add(f.getName() + " – tiltott (403)");
                        else if(ex.getStatusCode() == 404) failedNames.add(f.getName() + " – nem található (404)");
                        else failedNames.add(f.getName() + " – hiba: " + ex.getStatusCode());
                    }
                }

                final int d = deleted;
                final int f = failed;
                final List<String> fn = failedNames;

                javafx.application.Platform.runLater(() -> {
                    loadGoogleDriveFiles();
                    if(onDone != null) onDone.run();

                    StringBuilder msg = new StringBuilder();
                    msg.append("Törölve: ").append(d);

                    if(f > 0){
                        msg.append("\n\nNem törölhető: ").append(f).append("\n");
                        fn.forEach(n -> msg.append("• ").append(n).append("\n"));
                    }

                    Alert.AlertType type = (f == 0) ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
                    new Alert(type, msg.toString()).show();
                });

                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if(ex != null) ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Kritikus hiba:\n " + (ex != null ? ex.getMessage() : "ismeretlen")).show();
        });

        new Thread(task, "drive-delete-smart").start();
    }

    public void renameDriveFile(String fileId, String newName) throws IOException {
        if (driveManager == null) {
            try {
                driveManager = new GoogleDriveManager();
            } catch (GeneralSecurityException e) {
                throw new IOException(e);
            }
        }
        driveManager.renameFile(fileId, newName);
    }

    public boolean isDriveMode() {
        Disk selected = rootsMenu.getSelectionModel().getSelectedItem();
        return selected != null && "Google Drive".equals(selected.getPath());
    }

    public GoogleDriveManager getDriveManager() {
        return driveManager;
    }

    public void refreshView() {
        if (isDriveMode()) {
            loadGoogleDriveFiles();
        } else {
            loadFolders(currentDir);
        }
    }

    @FXML public void createFolder(){
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("Create Folder");
        dialog.setHeaderText("Create Folder");
        dialog.setContentText("Enter the folder's name:");
        dialog.showAndWait().ifPresent(folderName -> {
            if(folderName.isEmpty()) return;
            if (isDriveMode()) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        if (driveManager == null) driveManager = new GoogleDriveManager();
                        driveManager.createDriveFolder(folderName, currentFolderId);
                        return null;
                    }
                };

                task.setOnSucceeded(e -> {
                    loadGoogleDriveFiles(); // Frissítés
                    log.info("Drive folder created: " + folderName);
                });

                task.setOnFailed(e -> {
                    new Alert(Alert.AlertType.ERROR, "Could not create Drive folder!").show();
                    task.getException().printStackTrace();
                });

                new Thread(task).start();

            } else {
                File newDir = new File(currentDir, folderName);
                if (!newDir.exists()) {
                    if (newDir.mkdir()) {
                        loadFolders(currentDir);
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Failed to create local folder!").show();
                    }
                } else {
                    new Alert(Alert.AlertType.WARNING, "Folder already exists!").show();
                }
            }
        });

    }
}
