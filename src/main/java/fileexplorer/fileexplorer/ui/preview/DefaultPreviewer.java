package fileexplorer.fileexplorer.ui.preview;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.StorageProvider;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.net.URISyntaxException;

public class DefaultPreviewer implements FilePreview{
    @Override
    public boolean canHandle(StorageItem item) {
        return !item.isFolder();
    }

    @Override
    public Node createNode(StorageItem item, StorageProvider provider) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(20));

        Label iconLabel = new Label("\uD83D\uDCC4" + item.getName());
        iconLabel.setStyle("-fx-font-size: 32px");

        Button btn = new Button("Megnyitás");
        btn.setStyle("-fx-background-color: #3399FF; -fx-font-size: 20px; -fx-text-fill: white; -fx-font-grow: bold");

        btn.setOnAction(e->{
            try{
                java.io.File localFile = new java.io.File(item.getId());
                if(localFile.exists()){
                    Desktop.getDesktop().open(new java.io.File(item.getId()));
                }
                else{
                    String link = "https://drive.google.com/file/d/" + item.getId() + "/view";

                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new java.net.URI(link));
                    }
                }

            }
            catch (java.io.IOException e1){
                e1.printStackTrace();
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        box.getChildren().addAll(iconLabel, btn);
        return box;
    }
}
