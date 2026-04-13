package fileexplorer.fileexplorer.ui.preview;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.StorageProvider;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class FolderPreviewer implements FilePreview {

    @Override
    public boolean canHandle(StorageItem item) {
        return item.isFolder();
    }

    @Override
    public Node createNode(StorageItem item, StorageProvider provider) {
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("\uD83D\uDCC1 "+item.getName());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: black;");

        Label loadingLabel = new Label("Tartalom betöltése....");
        VBox filesList =  new VBox(5);

        mainBox.getChildren().addAll(title,loadingLabel);


        Task<List<StorageItem>> task =  new javafx.concurrent.Task<List<StorageItem>>() {

            @Override
            protected List<StorageItem> call() throws Exception {
                return provider.listContents(item.getId());
            }
        };

        task.setOnSucceeded(event -> {
            mainBox.getChildren().removeAll(loadingLabel);
            List<StorageItem> items = task.getValue();

            if(items!=null && !items.isEmpty()){
                for(StorageItem i : items){
                    String icon = i.isFolder() ? "\uD83D\uDCC1" : "\uD83D\uDCC4";
                    Label flabel = new Label(icon + i.getName());
                    filesList.getChildren().add(flabel);
                }
            }
            else{
                filesList.getChildren().add(new Label("A mappa üres"));
            }
        });

        task.setOnFailed(e -> {
            loadingLabel.setText("Hiba a betöltéskor: " + task.getException().getMessage());
        });

        new Thread(task).start();

        ScrollPane sp = new ScrollPane(filesList);
        sp.setFitToWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);
        mainBox.getChildren().add(sp);

        return mainBox;
    }
}
