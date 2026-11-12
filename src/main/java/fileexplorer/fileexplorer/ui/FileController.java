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

/**
 * Egyetlen fájlkezelő panel felhasználói felületét és interakcióit vezérlő JavaFX kontroller osztály.
 * Ez az osztály felelős a helyi fájlrendszer és a Google Drive felhőtárhelyei közötti váltásokat {@link StorageProvider},
 *  a könyvtárstruktúrák megjelenítéséért {@link Stack}, valamint az előnézeti megjelenítését {@link PreviewFactory}.
 *
 *  A hosszabb I/O- és hálózati műveleteket (listázás, törlés, átnevezés) háttérszálon futó
 *  JavaFX {@link Task} példányokon keresztül hajtja végre a felület fagyásának elkerülése érdekében,
 *  emellett getter metódusokon keresztül biztosítja a panel állapotát a {@code MainController} számára.
 */

public class FileController {
    @FXML public ComboBox<Disk> rootsMenu;
    @FXML public TextArea currentPath;
    @FXML public Button goBack;
    @FXML public Button copyButton;
    @FXML
    private Button deleteButton;
    @FXML public Button moveButton;
    @FXML public Button renameButton;
    @FXML public Button createFolder;
    @FXML public Button logoutButton;
    @FXML public ListView<StorageItem> folderList;
    @FXML public ListView<StorageItem> fileList;
    private PreviewFactory previewFactory = new PreviewFactory();
    private final FileOperationService fileService = new FileOperationService();

    private StorageProvider provider;
    private String currentPathId = null;
    private Stack<String> pathStack = new  Stack<>();
    private GoogleDriveManager manager;


    /**
     * Inicializálja a panel grafikus komponenseit és eseménykezelőit az FXML nézet betöltése után.
     * A metódus először a {@link #mainList()} hívásával konfigurálja a fájllista kijelölési módját
     * és navigációs eseményeit, majd lekérdezi a rendszer elérhető helyi gyökérkönyvtárait
     * ({@link File#listRoots()}), feltölti velük a meghajtóválasztó menüt ({@code rootsMenu}),
     * hozzáadja a {@code "Google Drive"} felhőtárhely opciót, és kiválasztja az első elérhető
     * meghajtót ({@link #onSelected()}).
     * Ezt követően kijelölésfigyelőt regisztrál a {@code folderList} listára, amely a kiválasztott
     * elemet átadja az előnézeti területnek ({@code fileList}), valamint egyedi cellagyárat
     * állít be a {@code fileList} számára, amely a {@link PreviewFactory} segítségével előállítja
     * az állományhoz illeszkedő előnézeti komponenst és annak méretét dinamikusan a rendelkezésre
     * álló helyhez köti.
     */
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

    /**
     * Ez a metódus konfigurálja a {@link folderList} megjelenítését.
     * Engedélyezi a többszörös kijelölést.
     * Egyedi ikonokat párosít, hogy a felhasználónak jobban átlátható legyen, hogy melyik állomány
     * egyszerú fájl és melyik a mappa.
     * Lekezeli a kattintásokat a listán megjelenített elemeken.
     */

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

    /**
     * Ez a metódus felelős a meghajtólistában történt választások lekezeléséért.
     * Amennyiben váltás történik, akkor elsősorban az alkalmazás törli az előzményeket az előző meghajtó után.
     * Ezután egy ellenőrzés következik, hogy a felhasználó a Google Drive meghajtót választotta-e, hiszen ha igen akkor
     * a kijelentkezési gombot átállítja aktívra, így a felhasználó ki tud jelentkezni majd. Az alkalmazás példányosít egy
     * {@code GoogleDriveManager} példányt
     * lekéri a bejelentkezett felhasználó e-mail-címét a menüelem megjelenített nevének
     * frissítéséhez, és beállítja a {@link DriverProvider} szolgáltatót.
     * Helyi meghajtó
     * esetén a {@link LocalProvider} kerül példányosításra a meghajtó gyökérútvonalával.
     * Végül frissíti az aktuális útvonalmezőt ({@code currentPath}) és elindítja a tartalom
     * betöltését ({@link #loadFiles()}).
     */

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
                currentPathId = "Google Drive";
                currentPath.setText(currentPathId);
                provider = new DriverProvider(manager);
                rootsMenu.getSelectionModel().getSelectedItem().setCostumeName("Google Drive - " + finalName);
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

    /**
     * Ez a metódus felelős az aktuális elérési útvonalon tárolt elemek betöltéséért.
     * Egy háttérszálon futó {@link Task} segítségével lekérdezi az aktív tárhely-szolgáltatótól
     * a {@link StorageItem} példányokat tároló listát, amely tartalmazza a tárolt állományok
     * adatait. Sikeres futás során beállítja a {@code folderList} elemeit és törli a
     * {@code fileList} tartalmát, hiba esetén pedig hibaüzenetet kapunk.
     */

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

    /**
     * Ez a metódus valósítja meg, hogy a könyvtárszerkezetben a visszalépés működjön.
     * Amennyiben a gyökérkönyvtárban van a felhasználó, azaz nem tud feljebb menni a
     * a könyvtárstruktúrában, ekkor egy hibaüzenetet kap a felhasználó.
     * Amennyiben a visszalépés megvalósítható, akkor az eddigi elérési útvonalakat tároló {@link Stack} elemből
     * betölti az utolsó elemet és visszalép.
     */
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

    /**
     * Kezeli a Google Drive fiókból történő kijelentkezést és a felület alaphelyzetbe állítását.
     * Amennyiben létezik aktív {@link GoogleDriveManager} példány, a metódus meghívja annak
     * {@link GoogleDriveManager#logout()} metódusát a munkamenet bontásához, majd {@code null}-ra
     * állítja a {@code manager} referenciát, hogy egy későbbi visszaváltáskor új hitelesítési
     * folyamat indulhasson el. Ezt követően visszaállítja a meghajtóválasztó menüben
     * ({@code rootsMenu}) megjelenített nevet az alapértelmezett {@code "Google Drive"}
     * feliratra, majd kiválasztja az első elérhető helyi meghajtót és frissíti a nézetet
     * ({@link #onSelected()}).
     */

    @FXML
    private void handleLogOut(){
        if (manager != null) {
            manager.logout();
            manager = null;
        }
        Disk selected = rootsMenu.getSelectionModel().getSelectedItem();
        if (selected != null && selected.toString().contains("Google Drive")) {
            selected.setCostumeName("Google Drive");
        }
        if (!rootsMenu.getItems().isEmpty()) {
            rootsMenu.getSelectionModel().select(0);
            onSelected();
        }
    }


    //Műveletek

    /**
     * Megerősítő párbeszédablak megjelenítése után háttérszálon törli a kijelölt elemeket
     * a {@link FileOperationService} segítségével, majd frissíti a nézetet ({@link #loadFiles()}).
     */

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

    /**
     * Bekéri egy párbeszédablakban az új nevet, majd ellenőrzi, hogy az átnevezendő elem egy mappa-e.
     * Ez azért fontos, mert amennyiben nem mappa, akkor egy fájlt szeretnénk átnevezni, azonban itt a kiterjesztésre
     * figyelni kell a fájl használhatóságának megtartása miatt.
     * Amennyiben az új névben egy új kiterjesztés található, a felhasználónak meg kell erősíteni, hogy szeretne kiterjesztést változtatni.
     * Ezután egy háttérszálon végrehajtja az átnevezést a {@link FileOperationService} segítségével.
     */
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

    /**
     * Egy párbeszéd ablakban bekéri az új mappának a nevét. Ezt ellenőrzi, hogy a felhasználó
     * adott-e meg nevet. Amennyiben igen, akkor az aktuális elérési útvonalon létrehoz egy új mappát, melynek beállítja
     * a megadott nevet.
     */

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

    /**
     * Visszaadja a panelhez tartozó providert.
     * @return az aktuális {@link StorageProvider} példány
     */
    public StorageProvider getProvider() {return  provider;}

    /**
     * Visszaadja az aktuáliis elérési útvonal azonosítóját.
     * @return az aktuális könyvtár azonosítója
     */
    public String getCurrentPathId() {return  currentPathId;}

    /**
     * Visszaadja a kijelölt elemek listáját.
     * @return a kijelölt {@link StorageItem} példányok listája
     */
    public List<StorageItem> getSelectedItems() {return folderList.getSelectionModel().getSelectedItems();}

    /**
     * Visszaad egyetlen kijelölt elem adatait.
     * @return egyetlen kijelölt elem adatai.
     */
    public StorageItem getSelectedItem() {return fileList.getSelectionModel().getSelectedItem();}

    /**
     * A deletebutton getter metódusa
     * @return a törlés {@link Button} komponense
     */
    public Button getDeleteButton() {return deleteButton;}

    /**
     * Beállítja a panelhez tartozó törlés gombot.
     * @param deleteButton a beállítandó törlés {@link Button}
     */
    public void setDeleteButton(Button deleteButton) {this.deleteButton = deleteButton;}
}